package com.hireconnect.interview.service.impl;

import com.hireconnect.interview.dto.request.RescheduleInterviewRequest;
import com.hireconnect.interview.dto.request.ScheduleInterviewRequest;
import com.hireconnect.interview.dto.response.InterviewResponse;
import com.hireconnect.interview.entity.Interview;
import com.hireconnect.interview.enums.InterviewStatus;
import com.hireconnect.interview.exception.ResourceNotFoundException;
import com.hireconnect.interview.mapper.InterviewMapper;
import com.hireconnect.interview.repository.InterviewRepository;
import com.hireconnect.interview.service.InterviewService;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InterviewServiceImpl implements InterviewService {
    private static final String REQUESTED_AT_PREFIX = "Requested date: ";
    private static final String PREVIOUS_STATUS_PREFIX = "Previous status: ";

    private final InterviewRepository interviewRepository;
    private final InterviewMapper interviewMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.notification}")
    private String notificationRoutingKey;

    @Override
    public InterviewResponse scheduleInterview(Long recruiterId, ScheduleInterviewRequest request) {
        log.info("Recruiter {} scheduling interview for application {}", recruiterId, request.getApplicationId());

        Interview interview = Interview.builder()
                .applicationId(request.getApplicationId())
                .candidateId(request.getCandidateId())
                .recruiterId(recruiterId)
                .jobId(request.getJobId())
                .jobTitle(request.getJobTitle())
                .candidateName(request.getCandidateName())
                .candidateEmail(request.getCandidateEmail())
                .companyName(request.getCompanyName())
                .scheduledAt(request.getScheduledAt())
                .mode(request.getMode())
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 60)
                .meetLink(request.getMeetLink())
                .location(request.getLocation())
                .notes(request.getNotes())
                .interviewerName(request.getInterviewerName())
                .roundNumber(request.getRoundNumber() != null ? request.getRoundNumber() : 1)
                .status(InterviewStatus.SCHEDULED)
                .build();

        Interview saved = interviewRepository.save(interview);
        log.info("Interview {} scheduled for candidate {} at {}", saved.getInterviewId(),
                request.getCandidateId(), request.getScheduledAt());

        publishInterviewEvent("INTERVIEW_SCHEDULED", saved);
        return toResponse(saved);
    }

    @Override
    public InterviewResponse confirmInterview(Long interviewId, Long candidateId) {
        Interview interview = findById(interviewId);

        if (!interview.getCandidateId().equals(candidateId)) {
            throw new AccessDeniedException("Only the candidate can confirm this interview");
        }

        if (interview.getStatus() != InterviewStatus.SCHEDULED &&
                interview.getStatus() != InterviewStatus.RESCHEDULED) {
            throw new IllegalStateException("Interview cannot be confirmed in status: " + interview.getStatus());
        }

        interview.setStatus(InterviewStatus.CONFIRMED);
        Interview updated = interviewRepository.save(interview);

        publishInterviewEvent("INTERVIEW_CONFIRMED", updated);
        return toResponse(updated);
    }

    @Override
    public InterviewResponse rescheduleInterview(Long interviewId, RescheduleInterviewRequest request, Long requesterId) {
        Interview interview = findById(interviewId);

        boolean isRecruiter = interview.getRecruiterId().equals(requesterId);

        if (!isRecruiter) {
            throw new AccessDeniedException("Only the assigned recruiter can reschedule this interview");
        }

        if (interview.getStatus() == InterviewStatus.CANCELLED ||
                interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new IllegalStateException("Cannot reschedule interview with status: " + interview.getStatus());
        }

        LocalDateTime newScheduledAt = request.getNewScheduledAt() != null
                ? request.getNewScheduledAt()
                : extractRequestedScheduledAt(interview.getRescheduleReason());

        if (newScheduledAt == null) {
            throw new IllegalStateException("New scheduled date/time is required");
        }

        interview.setScheduledAt(newScheduledAt);
        interview.setStatus(InterviewStatus.RESCHEDULED);
        interview.setRescheduleReason(cleanRescheduleReason(request.getRescheduleReason()));
        if (request.getMeetLink() != null) interview.setMeetLink(request.getMeetLink());
        if (request.getLocation() != null) interview.setLocation(request.getLocation());

        Interview updated = interviewRepository.save(interview);
        publishInterviewEvent("INTERVIEW_RESCHEDULED", updated);
        return toResponse(updated);
    }

    @Override
    public InterviewResponse cancelInterview(Long interviewId, Long requesterId, String reason) {
        Interview interview = findById(interviewId);

        boolean isCandidate = interview.getCandidateId().equals(requesterId);
        boolean isRecruiter = interview.getRecruiterId().equals(requesterId);

        if (!isCandidate && !isRecruiter) {
            throw new AccessDeniedException("You are not authorized to cancel this interview");
        }

        if (interview.getStatus() == InterviewStatus.COMPLETED ||
                interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new IllegalStateException("Interview is already " + interview.getStatus());
        }

        interview.setStatus(InterviewStatus.CANCELLED);
        interview.setCancellationReason(reason);

        Interview updated = interviewRepository.save(interview);
        publishInterviewEvent("INTERVIEW_CANCELLED", updated);
        return toResponse(updated);
    }

    @Override
    public InterviewResponse completeInterview(Long interviewId, Long recruiterId) {
        Interview interview = findById(interviewId);

        if (!interview.getRecruiterId().equals(recruiterId)) {
            throw new AccessDeniedException("Only the recruiter can mark interview as completed");
        }

        if (interview.getStatus() != InterviewStatus.CONFIRMED &&
                interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new IllegalStateException("Interview cannot be completed in status: " + interview.getStatus());
        }

        interview.setStatus(InterviewStatus.COMPLETED);
        Interview updated = interviewRepository.save(interview);
        return toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewResponse getInterviewById(Long interviewId) {
        return toResponse(findById(interviewId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getInterviewsByApplication(Long applicationId) {
        return interviewRepository.findByApplicationId(applicationId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InterviewResponse> getInterviewsByCandidate(Long candidateId, Pageable pageable) {
        return interviewRepository.findByCandidateId(candidateId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getUpcomingInterviewsByCandidate(Long candidateId) {
        return interviewRepository.findUpcomingByCandidate(candidateId, LocalDateTime.now())
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InterviewResponse> getInterviewsByRecruiter(Long recruiterId, Pageable pageable) {
        return interviewRepository.findByRecruiterId(recruiterId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getUpcomingInterviewsByRecruiter(Long recruiterId) {
        return interviewRepository.findUpcomingByRecruiter(recruiterId, LocalDateTime.now())
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InterviewResponse> getInterviewsByRecruiterAndStatus(Long recruiterId, InterviewStatus status, Pageable pageable) {
        return interviewRepository.findByRecruiterIdAndStatus(recruiterId, status, pageable)
                .map(this::toResponse);
    }

    @Override
    public InterviewResponse requestReschedule(Long interviewId, Long candidateId, RescheduleInterviewRequest request) {
        Interview interview = findById(interviewId);

        if (!interview.getCandidateId().equals(candidateId)) {
            throw new AccessDeniedException("Only the candidate can request to reschedule this interview");
        }

        if (interview.getStatus() != InterviewStatus.SCHEDULED &&
                interview.getStatus() != InterviewStatus.CONFIRMED &&
                interview.getStatus() != InterviewStatus.RESCHEDULED) {
            throw new IllegalStateException("Cannot request reschedule for interview in status: " + interview.getStatus());
        }

        if (request.getNewScheduledAt() == null) {
            throw new IllegalStateException("New scheduled date/time is required");
        }

        if (!request.getNewScheduledAt().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("New interview time must be in the future");
        }

        if (request.getRescheduleReason() == null || request.getRescheduleReason().isBlank()) {
            throw new IllegalStateException("Reschedule reason is required");
        }

        interview.setRescheduleReason(buildStoredRescheduleReason(
                request.getNewScheduledAt(),
                interview.getStatus(),
                request.getRescheduleReason()
        ));
        Interview updated = interviewRepository.save(interview);
        publishInterviewEvent("INTERVIEW_RESCHEDULE_REQUESTED", updated);
        return toResponse(updated);
    }

    @Override
    public InterviewResponse rejectReschedule(Long interviewId, Long recruiterId, String reason) {
        Interview interview = findById(interviewId);

        if (!interview.getRecruiterId().equals(recruiterId)) {
            throw new AccessDeniedException("Only the assigned recruiter can reject a reschedule request");
        }

        if (extractRequestedScheduledAt(interview.getRescheduleReason()) == null) {
            throw new IllegalStateException("Interview does not have a pending reschedule request");
        }

        interview.setStatus(extractPreviousStatus(interview.getRescheduleReason()));
        interview.setRescheduleReason(null);
        interview.setNotes(interview.getNotes() != null ? interview.getNotes() + "\nReschedule rejected: " + reason : "Reschedule rejected: " + reason);

        Interview updated = interviewRepository.save(interview);
        publishInterviewEvent("INTERVIEW_RESCHEDULE_REJECTED", updated);
        return toResponse(updated);
    }

    private Interview findById(Long interviewId) {
        return interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", interviewId));
    }

    private void publishInterviewEvent(String eventType, Interview interview) {
        try {
            LocalDateTime requestedScheduledAt = extractRequestedScheduledAt(interview.getRescheduleReason());
            Map<String, Object> event = Map.ofEntries(
                Map.entry("eventType", eventType),
                Map.entry("interviewId", safeLong(interview.getInterviewId())),
                Map.entry("applicationId", safeLong(interview.getApplicationId())),
                Map.entry("candidateId", safeLong(interview.getCandidateId())),
                Map.entry("recruiterId", safeLong(interview.getRecruiterId())),
                Map.entry("candidateEmail", interview.getCandidateEmail() != null ? interview.getCandidateEmail() : ""),
                Map.entry("candidateName", interview.getCandidateName() != null ? interview.getCandidateName() : ""),
                Map.entry("jobTitle", interview.getJobTitle() != null ? interview.getJobTitle() : ""),
                Map.entry("companyName", interview.getCompanyName() != null ? interview.getCompanyName() : ""),
                Map.entry("scheduledAt", interview.getScheduledAt() != null ? interview.getScheduledAt().toString() : ""),
                Map.entry("requestedScheduledAt", requestedScheduledAt != null ? requestedScheduledAt.toString() : ""),
                Map.entry("mode", interview.getMode() != null ? interview.getMode().name() : ""),
                Map.entry("status", interview.getStatus() != null ? interview.getStatus().name() : "")
            );
            rabbitTemplate.convertAndSend(exchange, notificationRoutingKey, event);
        } catch (Exception e) {
            log.error("Failed to publish interview event {}: {}", eventType, e.getMessage());
        }
    }

    private InterviewResponse toResponse(Interview interview) {
        InterviewResponse response = interviewMapper.toResponse(interview);
        LocalDateTime requestedScheduledAt = extractRequestedScheduledAt(interview.getRescheduleReason());
        response.setRequestedScheduledAt(requestedScheduledAt);
        response.setStatusBeforeRescheduleRequest(extractPreviousStatus(interview.getRescheduleReason()));
        response.setRescheduleReason(cleanRescheduleReason(interview.getRescheduleReason()));
        if (requestedScheduledAt != null) {
            response.setStatus(InterviewStatus.RESCHEDULE_REQUESTED);
        }
        return response;
    }

    private String buildStoredRescheduleReason(LocalDateTime requestedAt, InterviewStatus previousStatus, String reason) {
        return REQUESTED_AT_PREFIX + requestedAt + "\n" +
                PREVIOUS_STATUS_PREFIX + previousStatus + "\n" +
                reason.trim();
    }

    private LocalDateTime extractRequestedScheduledAt(String storedReason) {
        if (storedReason == null || !storedReason.startsWith(REQUESTED_AT_PREFIX)) {
            return null;
        }

        String firstLine = storedReason.lines().findFirst().orElse("");
        String value = firstLine.substring(REQUESTED_AT_PREFIX.length()).trim();

        try {
            return LocalDateTime.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private InterviewStatus extractPreviousStatus(String storedReason) {
        if (storedReason == null) {
            return InterviewStatus.SCHEDULED;
        }

        return storedReason.lines()
                .filter(line -> line.startsWith(PREVIOUS_STATUS_PREFIX))
                .findFirst()
                .map(line -> line.substring(PREVIOUS_STATUS_PREFIX.length()).trim())
                .map(value -> {
                    try {
                        return InterviewStatus.valueOf(value);
                    } catch (Exception ex) {
                        return InterviewStatus.SCHEDULED;
                    }
                })
                .orElse(InterviewStatus.SCHEDULED);
    }

    private String cleanRescheduleReason(String storedReason) {
        if (storedReason == null) {
            return null;
        }

        return storedReason.lines()
                .filter(line -> !line.startsWith(REQUESTED_AT_PREFIX))
                .filter(line -> !line.startsWith(PREVIOUS_STATUS_PREFIX))
                .reduce((first, second) -> first + "\n" + second)
                .orElse("")
                .trim();
    }

    private Long safeLong(Long value) {
        return value != null ? value : 0L;
    }
}
