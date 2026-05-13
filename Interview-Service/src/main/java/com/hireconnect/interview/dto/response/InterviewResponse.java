package com.hireconnect.interview.dto.response;

import com.hireconnect.interview.enums.InterviewMode;
import com.hireconnect.interview.enums.InterviewStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewResponse {
    private Long interviewId;
    private Long applicationId;
    private Long candidateId;
    private Long recruiterId;
    private Long jobId;
    private String jobTitle;
    private String candidateName;
    private String candidateEmail;
    private String companyName;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private InterviewMode mode;
    private String meetLink;
    private String location;
    private InterviewStatus status;
    private String notes;
    private String cancellationReason;
    private String rescheduleReason;
    private LocalDateTime requestedScheduledAt;
    private InterviewStatus statusBeforeRescheduleRequest;
    private String interviewerName;
    private Integer roundNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
