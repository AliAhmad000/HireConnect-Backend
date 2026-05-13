package com.hireconnect.application.service.impl;

import com.hireconnect.application.dto.request.SubmitApplicationRequest;
import com.hireconnect.application.dto.request.RecruiterMessageRequest;
import com.hireconnect.application.dto.request.UpdateStatusRequest;
import com.hireconnect.application.dto.response.ApplicationResponse;
import com.hireconnect.application.entity.Application;
import com.hireconnect.application.enums.ApplicationStatus;
import com.hireconnect.application.client.JobServiceClient;
import com.hireconnect.application.exception.DuplicateApplicationException;
import com.hireconnect.application.exception.InvalidStatusTransitionException;
import com.hireconnect.application.exception.ResourceNotFoundException;
import com.hireconnect.application.mapper.ApplicationMapper;
import com.hireconnect.application.repository.ApplicationRepository;
import com.hireconnect.application.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;
    private final RabbitTemplate rabbitTemplate;
    private final JobServiceClient jobServiceClient;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.notification}")
    private String notificationRoutingKey;

    @Value("${app.rabbitmq.routing-key.analytics}")
    private String analyticsRoutingKey;

    // Valid status transitions
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> VALID_TRANSITIONS = Map.of(
        ApplicationStatus.APPLIED, Set.of(ApplicationStatus.SHORTLISTED, ApplicationStatus.REJECTED),
        ApplicationStatus.SHORTLISTED, Set.of(ApplicationStatus.INTERVIEW_SCHEDULED, ApplicationStatus.REJECTED),
        ApplicationStatus.INTERVIEW_SCHEDULED, Set.of(ApplicationStatus.OFFERED, ApplicationStatus.REJECTED),
        ApplicationStatus.OFFERED, Set.of(),
        ApplicationStatus.REJECTED, Set.of(),
        ApplicationStatus.WITHDRAWN, Set.of()
    );

    @Override
    public ApplicationResponse submitApplication(Long candidateId, String candidateName,
                                                  String candidateEmail, SubmitApplicationRequest request) {
        log.info("Candidate {} submitting application for job {}", candidateId, request.getJobId());

        if (applicationRepository.existsByJobIdAndCandidateId(request.getJobId(), candidateId)) {
            throw new DuplicateApplicationException(request.getJobId(), candidateId);
        }

        Long recruiterId = request.getRecruiterId();
        if (recruiterId == null) {
            recruiterId = jobServiceClient.getRecruiterIdByJob(request.getJobId()).getData();
        }

        Application application = Application.builder()
                .jobId(request.getJobId())
                .candidateId(candidateId)
                .recruiterId(recruiterId)
                .jobTitle(request.getJobTitle())
                .companyName(request.getCompanyName())
                .candidateName(candidateName != null && !candidateName.isBlank() ? candidateName : candidateEmail)
                .candidateEmail(candidateEmail)
                .coverLetter(request.getCoverLetter())
                .resumeUrl(request.getResumeUrl())
                .status(ApplicationStatus.APPLIED)
                .isWithdrawn(false)
                .build();

        Application saved = applicationRepository.save(application);
        log.info("Application {} submitted by candidate {}", saved.getApplicationId(), candidateId);

        // Publish event to RabbitMQ
        publishApplicationEvent("APPLICATION_SUBMITTED", saved);

        return applicationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long applicationId) {
        Application application = findApplicationById(applicationId);
        return applicationMapper.toResponse(application);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getApplicationsByCandidate(Long candidateId, Pageable pageable) {
        return applicationRepository.findByCandidateId(candidateId, pageable)
                .map(applicationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAllApplicationsByCandidate(Long candidateId) {
        return applicationRepository.findByCandidateIdOrderByAppliedAtDesc(candidateId)
                .stream()
                .map(applicationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getApplicationsByJob(Long jobId, Pageable pageable) {
        return applicationRepository.findByJobId(jobId, pageable)
                .map(applicationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getApplicationsByJobAndStatus(Long jobId, ApplicationStatus status, Pageable pageable) {
        return applicationRepository.findByJobIdAndStatus(jobId, status, pageable)
                .map(applicationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getApplicationsByRecruiter(Long recruiterId, Pageable pageable) {
        return applicationRepository.findByRecruiterId(recruiterId, pageable)
                .map(applicationMapper::toResponse);
    }

    @Override
    public ApplicationResponse updateApplicationStatus(Long applicationId, UpdateStatusRequest request, Long recruiterId) {
        Application application = findApplicationById(applicationId);

        if (!application.getRecruiterId().equals(recruiterId)) {
            throw new AccessDeniedException("You are not authorized to update this application");
        }

        if (Boolean.TRUE.equals(application.getIsWithdrawn())) {
            throw new IllegalStateException("Cannot update status of a withdrawn application");
        }

        validateStatusTransition(application.getStatus(), request.getStatus());

        ApplicationStatus oldStatus = application.getStatus();
        application.setStatus(request.getStatus());
        application.setStatusUpdatedAt(LocalDateTime.now());

        if (request.getRejectionReason() != null && request.getStatus() == ApplicationStatus.REJECTED) {
            application.setRejectionReason(request.getRejectionReason());
        }

        Application updated = applicationRepository.save(application);
        log.info("Application {} status updated from {} to {} by recruiter {}",
                applicationId, oldStatus, request.getStatus(), recruiterId);

        publishStatusChangeEvent(updated, oldStatus);

        return applicationMapper.toResponse(updated);
    }

    @Override
    public void sendMessageToCandidate(Long applicationId, RecruiterMessageRequest request, Long recruiterId) {
        Application application = findApplicationById(applicationId);

        if (!application.getRecruiterId().equals(recruiterId)) {
            throw new AccessDeniedException("You are not authorized to message this candidate");
        }

        if (Boolean.TRUE.equals(application.getIsWithdrawn())) {
            throw new IllegalStateException("Cannot message a withdrawn application");
        }

        if (!canRecruiterMessageCandidate(application.getStatus())) {
            throw new IllegalStateException("Messages can be sent after the candidate is shortlisted, scheduled, or offered");
        }

        publishCandidateMessageEvent(application, request.getMessage());
        log.info("Recruiter {} sent portal message for application {}", recruiterId, applicationId);
    }

    @Override
    public void withdrawApplication(Long applicationId, Long candidateId) {
        Application application = findApplicationById(applicationId);

        if (!application.getCandidateId().equals(candidateId)) {
            throw new AccessDeniedException("You are not authorized to withdraw this application");
        }

        if (Boolean.TRUE.equals(application.getIsWithdrawn())) {
            throw new IllegalStateException("Application is already withdrawn");
        }

        if (application.getStatus() == ApplicationStatus.OFFERED ||
                application.getStatus() == ApplicationStatus.REJECTED) {
            throw new IllegalStateException("Cannot withdraw an application with status: " + application.getStatus());
        }

        application.setIsWithdrawn(true);
        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setStatusUpdatedAt(LocalDateTime.now());

        applicationRepository.save(application);
        log.info("Application {} withdrawn by candidate {}", applicationId, candidateId);

        publishApplicationEvent("APPLICATION_WITHDRAWN", application);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasApplied(Long jobId, Long candidateId) {
        return applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countApplicationsByJob(Long jobId) {
        return applicationRepository.countByJobId(jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countApplicationsByCandidate(Long candidateId) {
        return applicationRepository.countByCandidateId(candidateId);
    }

    private Application findApplicationById(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));
    }

    private void validateStatusTransition(ApplicationStatus from, ApplicationStatus to) {
        Set<ApplicationStatus> allowedTransitions = VALID_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowedTransitions.contains(to)) {
            throw new InvalidStatusTransitionException(from.name(), to.name());
        }
    }

    private void publishApplicationEvent(String eventType, Application application) {
        try {
            Map<String, Object> event = Map.ofEntries(
                Map.entry("eventType", eventType),
                Map.entry("applicationId", application.getApplicationId()),
                Map.entry("candidateId", application.getCandidateId()),
                Map.entry("candidateEmail", application.getCandidateEmail() != null ? application.getCandidateEmail() : ""),
                Map.entry("candidateName", application.getCandidateName() != null ? application.getCandidateName() : ""),
                Map.entry("recruiterId", application.getRecruiterId()),
                Map.entry("jobId", application.getJobId()),
                Map.entry("jobTitle", application.getJobTitle() != null ? application.getJobTitle() : ""),
                Map.entry("companyName", application.getCompanyName() != null ? application.getCompanyName() : ""),
                Map.entry("status", application.getStatus().name())
            );
            rabbitTemplate.convertAndSend(exchange, notificationRoutingKey, event);
            log.debug("Published event: {} for application: {}", eventType, application.getApplicationId());
        } catch (Exception e) {
            log.error("Failed to publish event {} for application {}: {}",
                    eventType, application.getApplicationId(), e.getMessage());
        }
    }

    private void publishStatusChangeEvent(Application application, ApplicationStatus oldStatus) {
        try {
            Map<String, Object> event = Map.ofEntries(
                Map.entry("eventType", "APPLICATION_STATUS_CHANGED"),
                Map.entry("applicationId", application.getApplicationId()),
                Map.entry("candidateId", application.getCandidateId()),
                Map.entry("candidateEmail", application.getCandidateEmail() != null ? application.getCandidateEmail() : ""),
                Map.entry("candidateName", application.getCandidateName() != null ? application.getCandidateName() : ""),
                Map.entry("recruiterId", application.getRecruiterId()),
                Map.entry("jobId", application.getJobId()),
                Map.entry("jobTitle", application.getJobTitle() != null ? application.getJobTitle() : ""),
                Map.entry("companyName", application.getCompanyName() != null ? application.getCompanyName() : ""),
                Map.entry("oldStatus", oldStatus.name()),
                Map.entry("newStatus", application.getStatus().name()),
                Map.entry("status", application.getStatus().name()),
                Map.entry("rejectionReason", application.getRejectionReason() != null ? application.getRejectionReason() : "")
            );
            rabbitTemplate.convertAndSend(exchange, notificationRoutingKey, event);
            rabbitTemplate.convertAndSend(exchange, analyticsRoutingKey, event);
        } catch (Exception e) {
            log.error("Failed to publish status change event: {}", e.getMessage());
        }
    }

    private void publishCandidateMessageEvent(Application application, String message) {
        try {
            Map<String, Object> event = Map.ofEntries(
                Map.entry("eventType", "CANDIDATE_MESSAGE"),
                Map.entry("applicationId", application.getApplicationId()),
                Map.entry("candidateId", application.getCandidateId()),
                Map.entry("candidateEmail", application.getCandidateEmail() != null ? application.getCandidateEmail() : ""),
                Map.entry("candidateName", application.getCandidateName() != null ? application.getCandidateName() : ""),
                Map.entry("recruiterId", application.getRecruiterId()),
                Map.entry("jobId", application.getJobId()),
                Map.entry("jobTitle", application.getJobTitle() != null ? application.getJobTitle() : ""),
                Map.entry("companyName", application.getCompanyName() != null ? application.getCompanyName() : ""),
                Map.entry("message", message)
            );
            rabbitTemplate.convertAndSend(exchange, notificationRoutingKey, event);
        } catch (Exception e) {
            log.error("Failed to publish candidate message for application {}: {}",
                    application.getApplicationId(), e.getMessage());
            throw new IllegalStateException("Unable to send message right now");
        }
    }

    private boolean canRecruiterMessageCandidate(ApplicationStatus status) {
        return status == ApplicationStatus.SHORTLISTED
                || status == ApplicationStatus.INTERVIEW_SCHEDULED
                || status == ApplicationStatus.OFFERED;
    }
}
