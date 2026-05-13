package com.hireconnect.application.entity;

import com.hireconnect.application.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "applications",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"job_id", "candidate_id"},
        name = "uk_application_job_candidate"
    ),
    indexes = {
        @Index(name = "idx_application_candidate", columnList = "candidate_id"),
        @Index(name = "idx_application_job", columnList = "job_id"),
        @Index(name = "idx_application_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"coverLetter"})
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicationId;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "recruiter_id", nullable = false)
    private Long recruiterId;

    @Column(name = "applied_at", nullable = false)
    private LocalDate appliedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "resume_url", nullable = false)
    private String resumeUrl;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "candidate_name")
    private String candidateName;

    @Column(name = "candidate_email")
    private String candidateEmail;

    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "is_withdrawn", nullable = false)
    @Builder.Default
    private Boolean isWithdrawn = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (appliedAt == null) appliedAt = LocalDate.now();
        if (status == null) status = ApplicationStatus.APPLIED;
        if (isWithdrawn == null) isWithdrawn = false;
    }
}
