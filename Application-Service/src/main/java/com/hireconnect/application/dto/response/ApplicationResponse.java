package com.hireconnect.application.dto.response;

import com.hireconnect.application.enums.ApplicationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ApplicationResponse {
    private Long applicationId;
    private Long jobId;
    private Long candidateId;
    private Long recruiterId;
    private String jobTitle;
    private String companyName;
    private String candidateName;
    private String candidateEmail;
    private ApplicationStatus status;
    private String coverLetter;
    private String resumeUrl;
    private LocalDate appliedAt;
    private LocalDateTime statusUpdatedAt;
    private String rejectionReason;
    private Boolean isWithdrawn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
