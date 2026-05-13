package com.hireconnect.job.entity;

import com.hireconnect.job.enums.JobStatus;
import com.hireconnect.job.enums.JobType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "jobs",
    indexes = {
        @Index(name = "idx_job_posted_by",  columnList = "posted_by"),
        @Index(name = "idx_job_status",     columnList = "status"),
        @Index(name = "idx_job_category",   columnList = "category"),
        @Index(name = "idx_job_location",   columnList = "location"),
        @Index(name = "idx_job_type",       columnList = "job_type"),
        @Index(name = "idx_job_posted_at",  columnList = "posted_at")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long jobId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 20)
    private JobType jobType;

    @Column(nullable = false)
    private String location;

    @Column(name = "salary_min")
    private Double salaryMin;

    @Column(name = "salary_max")
    private Double salaryMax;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @ElementCollection
    @CollectionTable(name = "job_skills", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "skill")
    @Builder.Default
    private List<String> skills = new ArrayList<>();

    @Column(name = "experience_required")
    private Integer experienceRequired;

    @Column(name = "vacancies")
    @Builder.Default
    private Integer vacancies = 1;

    @Column(name = "posted_by", nullable = false)
    private Long postedBy;                // recruiterId

    @Column(name = "company_name")
    private String companyName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private JobStatus status = JobStatus.ACTIVE;

    @Column(name = "posted_at")
    private LocalDate postedAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "is_remote")
    @Builder.Default
    private Boolean isRemote = false;

    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (postedAt == null) postedAt = LocalDate.now();
        if (status == null)   status = JobStatus.ACTIVE;
        if (vacancies == null) vacancies = 1;
        if (isRemote == null) isRemote = false;
        if (viewCount == null) viewCount = 0L;
    }
}
