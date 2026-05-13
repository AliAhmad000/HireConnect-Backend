package com.hireconnect.job.service.impl;

import com.hireconnect.job.client.ProfileServiceClient;
import com.hireconnect.job.client.SubscriptionServiceClient;
import com.hireconnect.job.dto.request.JobRequest;
import com.hireconnect.job.dto.response.ApiResponse;
import com.hireconnect.job.dto.response.CandidateNotificationRecipientResponse;
import com.hireconnect.job.dto.response.JobResponse;
import com.hireconnect.job.entity.Job;
import com.hireconnect.job.enums.JobStatus;
import com.hireconnect.job.enums.JobType;
import com.hireconnect.job.exception.ResourceNotFoundException;
import com.hireconnect.job.repository.JobRepository;
import com.hireconnect.job.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ProfileServiceClient profileServiceClient;
    private final SubscriptionServiceClient subscriptionServiceClient;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.analytics}")
    private String analyticsRoutingKey;

    @Value("${app.rabbitmq.routing-key.notification}")
    private String notificationRoutingKey;

    @Override
    public JobResponse createJob(Long recruiterId, String companyName, JobRequest request) {
        log.info("Creating job: title={}, recruiterId={}", request.getTitle(), recruiterId);
        JobStatus requestedStatus = request.getStatus() != null ? request.getStatus() : JobStatus.ACTIVE;
        if (requestedStatus == JobStatus.ACTIVE) {
            validatePublishableJob(request);
            enforceJobPostLimit(recruiterId);
        }
        Job job = Job.builder()
                .title(defaultText(request.getTitle(), "Untitled Draft"))
                .category(defaultText(request.getCategory(), "Uncategorized"))
                .jobType(request.getJobType())
                .location(defaultText(request.getLocation(), "Not specified"))
                .salaryMin(request.getSalaryMin() != null ? request.getSalaryMin() : 0.0)
                .salaryMax(request.getSalaryMax() != null ? request.getSalaryMax() : 0.0)
                .description(defaultText(request.getDescription(), "Draft job description pending."))
                .skills(request.getSkills() != null ? request.getSkills() : List.of())
                .experienceRequired(request.getExperienceRequired())
                .vacancies(request.getVacancies() != null ? request.getVacancies() : 1)
                .postedBy(recruiterId)
                .companyName(defaultText(request.getCompanyName(), companyName != null ? companyName : "Not specified"))
                .isRemote(request.getIsRemote() != null ? request.getIsRemote() : false)
                .expiresAt(request.getExpiresAt())
                .status(requestedStatus)
                .build();
        Job saved = jobRepository.save(job);
        log.info("Job {} created by recruiter {}", saved.getJobId(), recruiterId);
        if (saved.getStatus() == JobStatus.ACTIVE) {
            publishNewJobAlerts(saved);
        }
        return toResponse(saved);
    }

    @Override
    public JobResponse getJobById(Long jobId, Long viewerId, String viewerRole) {
        Job job = findJobById(jobId);
        // Increment view count and publish analytics event asynchronously
        jobRepository.incrementViewCount(jobId);
        publishViewEvent(jobId, job.getPostedBy(), viewerId, viewerRole);
        return toResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> getAllActiveJobs(Pageable pageable) {
        return jobRepository.findByStatus(JobStatus.ACTIVE, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> getAllJobs(Pageable pageable, JobStatus status) {
        if (status != null) {
            return jobRepository.findByStatus(status, pageable).map(this::toResponse);
        }

        return jobRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> searchJobs(String title, String location, String category,
                                         JobType jobType, Integer experience,
                                         Double minSalary, Double maxSalary, Pageable pageable) {
        return jobRepository.searchJobs(title, location, category, jobType,
                experience, minSalary, maxSalary, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> getJobsByRecruiter(Long recruiterId, Pageable pageable) {
        return jobRepository.findByPostedBy(recruiterId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> getJobsByRecruiterAndStatus(Long recruiterId, JobStatus status, Pageable pageable) {
        return jobRepository.findByPostedByAndStatus(recruiterId, status, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getAllJobsByRecruiter(Long recruiterId) {
        return jobRepository.findByPostedByOrderByCreatedAtDesc(recruiterId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public JobResponse updateJob(Long jobId, Long recruiterId, JobRequest request) {
        Job job = findJobById(jobId);
        verifyOwnership(job, recruiterId);
        if (request.getTitle() != null)              job.setTitle(request.getTitle());
        if (request.getCategory() != null)           job.setCategory(request.getCategory());
        if (request.getJobType() != null)            job.setJobType(request.getJobType());
        if (request.getLocation() != null)           job.setLocation(request.getLocation());
        if (request.getSalaryMin() != null)          job.setSalaryMin(request.getSalaryMin());
        if (request.getSalaryMax() != null)          job.setSalaryMax(request.getSalaryMax());
        if (request.getDescription() != null)        job.setDescription(request.getDescription());
        if (request.getSkills() != null)             job.setSkills(request.getSkills());
        if (request.getExperienceRequired() != null) job.setExperienceRequired(request.getExperienceRequired());
        if (request.getVacancies() != null)          job.setVacancies(request.getVacancies());
        if (request.getCompanyName() != null)        job.setCompanyName(request.getCompanyName());
        if (request.getIsRemote() != null)           job.setIsRemote(request.getIsRemote());
        if (request.getExpiresAt() != null)          job.setExpiresAt(request.getExpiresAt());
        if (request.getStatus() != null) {
            if (request.getStatus() == JobStatus.ACTIVE && job.getStatus() != JobStatus.ACTIVE) {
                validatePublishableJob(request);
                enforceJobPostLimit(recruiterId);
            }
            job.setStatus(request.getStatus());
        }
        return toResponse(jobRepository.save(job));
    }

    @Override
    public JobResponse updateJobStatus(Long jobId, Long recruiterId, JobStatus status) {
        Job job = findJobById(jobId);
        verifyOwnership(job, recruiterId);
        if (status == JobStatus.ACTIVE && job.getStatus() != JobStatus.ACTIVE) {
            validatePublishableJob(job);
            enforceJobPostLimit(recruiterId);
        }
        job.setStatus(status);
        log.info("Job {} status updated to {} by recruiter {}", jobId, status, recruiterId);
        return toResponse(jobRepository.save(job));
    }

    @Override
    public JobResponse updateJobStatusAsAdmin(Long jobId, JobStatus status) {
        Job job = findJobById(jobId);
        job.setStatus(status);
        log.info("Job {} status updated to {} by admin", jobId, status);
        return toResponse(jobRepository.save(job));
    }

    @Override
    public void deleteJob(Long jobId, Long recruiterId) {
        Job job = findJobById(jobId);
        verifyOwnership(job, recruiterId);
        jobRepository.delete(job);
        log.info("Job {} deleted by recruiter {}", jobId, recruiterId);
    }

    @Override
    public void deleteJobAsAdmin(Long jobId) {
        Job job = findJobById(jobId);
        jobRepository.delete(job);
        log.info("Job {} deleted by admin", jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean jobExists(Long jobId) {
        return jobRepository.existsById(jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getRecruiterIdByJob(Long jobId) {
        return findJobById(jobId).getPostedBy();
    }

    @Override
    @Transactional(readOnly = true)
    public long countJobsByRecruiter(Long recruiterId) {
        return jobRepository.countByPostedBy(recruiterId);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private Job findJobById(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
    }

    private void verifyOwnership(Job job, Long recruiterId) {
        if (!job.getPostedBy().equals(recruiterId)) {
            throw new AccessDeniedException("You are not authorized to modify this job posting");
        }
    }

    private void publishViewEvent(Long jobId, Long recruiterId, Long viewerId, String viewerRole) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType",  "JOB_VIEWED",
                    "jobId",      jobId,
                    "recruiterId", recruiterId,
                    "viewerId",   viewerId != null ? viewerId : 0L,
                    "viewerRole", viewerRole != null ? viewerRole : "GUEST"
            );
            rabbitTemplate.convertAndSend(exchange, analyticsRoutingKey, event);
        } catch (Exception e) {
            log.warn("Failed to publish JOB_VIEWED event for jobId {}: {}", jobId, e.getMessage());
        }
    }

    private void publishNewJobAlerts(Job job) {
        try {
            ApiResponse<List<CandidateNotificationRecipientResponse>> response =
                    profileServiceClient.getCandidateNotificationRecipients();
            List<CandidateNotificationRecipientResponse> recipients =
                    response != null && response.getData() != null ? response.getData() : List.of();

            for (CandidateNotificationRecipientResponse recipient : recipients) {
                if (recipient.getUserId() == null || recipient.getEmail() == null || recipient.getEmail().isBlank()) {
                    continue;
                }

                Map<String, Object> event = Map.of(
                        "eventType", "NEW_JOB_ALERT",
                        "candidateId", recipient.getUserId(),
                        "candidateEmail", recipient.getEmail(),
                        "candidateName", recipient.getFullName() != null ? recipient.getFullName() : "",
                        "jobId", job.getJobId(),
                        "jobTitle", job.getTitle(),
                        "companyName", job.getCompanyName() != null ? job.getCompanyName() : "",
                        "location", job.getLocation() != null ? job.getLocation() : "",
                        "jobType", job.getJobType() != null ? job.getJobType().name() : "",
                        "isRemote", Boolean.TRUE.equals(job.getIsRemote())
                );
                rabbitTemplate.convertAndSend(exchange, notificationRoutingKey, event);
            }

            log.info("Published NEW_JOB_ALERT notifications for jobId={} to {} candidates",
                    job.getJobId(), recipients.size());
        } catch (Exception e) {
            log.warn("Failed to publish NEW_JOB_ALERT events for jobId {}: {}", job.getJobId(), e.getMessage());
        }
    }

    private void enforceJobPostLimit(Long recruiterId) {
        int maxPosts = 3;
        try {
            ApiResponse<Integer> response = subscriptionServiceClient.getMaxJobPosts(recruiterId);
            if (response != null && response.getData() != null) {
                maxPosts = response.getData();
            }
        } catch (Exception e) {
            log.warn("Could not load subscription limits for recruiter {}. Falling back to free limit: {}",
                    recruiterId, e.getMessage());
        }

        long activePosts = jobRepository.countByPostedByAndStatus(recruiterId, JobStatus.ACTIVE);
        if (activePosts >= maxPosts) {
            throw new IllegalStateException(
                    "Job post limit reached for your current subscription. Upgrade your plan to post more jobs.");
        }
    }

    private void validatePublishableJob(JobRequest request) {
        if (isBlank(request.getTitle())
                || isBlank(request.getCategory())
                || request.getJobType() == null
                || isBlank(request.getLocation())
                || request.getSalaryMin() == null
                || request.getSalaryMax() == null
                || isBlank(request.getDescription())
                || request.getDescription().length() < 50
                || request.getVacancies() == null
                || request.getVacancies() < 1
                || isBlank(request.getCompanyName())
                || request.getExpiresAt() == null) {
            throw new IllegalArgumentException("Please complete all required job fields before publishing.");
        }
    }

    private void validatePublishableJob(Job job) {
        if (isBlank(job.getTitle())
                || isBlank(job.getCategory())
                || job.getJobType() == null
                || isBlank(job.getLocation())
                || job.getSalaryMin() == null
                || job.getSalaryMax() == null
                || isBlank(job.getDescription())
                || job.getDescription().length() < 50
                || job.getVacancies() == null
                || job.getVacancies() < 1
                || isBlank(job.getCompanyName())
                || job.getExpiresAt() == null) {
            throw new IllegalArgumentException("Please complete all required job fields before publishing.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private JobResponse toResponse(Job j) {
        return JobResponse.builder()
                .jobId(j.getJobId()).title(j.getTitle()).category(j.getCategory())
                .jobType(j.getJobType()).location(j.getLocation())
                .salaryMin(j.getSalaryMin()).salaryMax(j.getSalaryMax())
                .description(j.getDescription()).skills(j.getSkills())
                .experienceRequired(j.getExperienceRequired()).vacancies(j.getVacancies())
                .postedBy(j.getPostedBy()).companyName(j.getCompanyName())
                .status(j.getStatus()).postedAt(j.getPostedAt()).expiresAt(j.getExpiresAt())
                .isRemote(j.getIsRemote()).viewCount(j.getViewCount())
                .createdAt(j.getCreatedAt()).updatedAt(j.getUpdatedAt())
                .build();
    }
}
