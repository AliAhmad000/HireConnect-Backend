package com.hireconnect.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "job_view_events",
    indexes = {
        @Index(name = "idx_view_job", columnList = "job_id"),
        @Index(name = "idx_view_recruiter", columnList = "recruiter_id"),
        @Index(name = "idx_view_created_at", columnList = "created_at")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobViewEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "recruiter_id")
    private Long recruiterId;

    @Column(name = "viewer_id")
    private Long viewerId;

    @Column(name = "viewer_role", length = 20)
    private String viewerRole;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
