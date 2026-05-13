package com.hireconnect.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "application_events",
    indexes = {
        @Index(name = "idx_app_event_job",       columnList = "job_id"),
        @Index(name = "idx_app_event_recruiter",  columnList = "recruiter_id"),
        @Index(name = "idx_app_event_candidate",  columnList = "candidate_id"),
        @Index(name = "idx_app_event_type",       columnList = "event_type"),
        @Index(name = "idx_app_event_created_at", columnList = "created_at")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "recruiter_id")
    private Long recruiterId;

    @Column(name = "candidate_id")
    private Long candidateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;      // APPLICATION_SUBMITTED, STATUS_CHANGED, WITHDRAWN

    @Column(name = "old_status", length = 30)
    private String oldStatus;

    @Column(name = "new_status", length = 30)
    private String newStatus;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
