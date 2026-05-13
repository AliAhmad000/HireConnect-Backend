package com.hireconnect.interview.entity;

import com.hireconnect.interview.enums.InterviewMode;
import com.hireconnect.interview.enums.InterviewStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "interviews",
    indexes = {
        @Index(name = "idx_interview_application", columnList = "application_id"),
        @Index(name = "idx_interview_candidate", columnList = "candidate_id"),
        @Index(name = "idx_interview_recruiter", columnList = "recruiter_id"),
        @Index(name = "idx_interview_status", columnList = "status"),
        @Index(name = "idx_interview_scheduled_at", columnList = "scheduled_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long interviewId;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "recruiter_id", nullable = false)
    private Long recruiterId;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "candidate_name")
    private String candidateName;

    @Column(name = "candidate_email")
    private String candidateEmail;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "duration_minutes")
    @Builder.Default
    private Integer durationMinutes = 60;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewMode mode;

    @Column(name = "meet_link")
    private String meetLink;

    @Column(name = "location")
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "reschedule_reason")
    private String rescheduleReason;

    @Column(name = "interviewer_name")
    private String interviewerName;

    @Column(name = "round_number")
    @Builder.Default
    private Integer roundNumber = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = InterviewStatus.SCHEDULED;
        if (durationMinutes == null) durationMinutes = 60;
        if (roundNumber == null) roundNumber = 1;
    }
}
