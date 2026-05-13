package com.hireconnect.subscription.entity;

import com.hireconnect.subscription.enums.SubscriptionPlan;
import com.hireconnect.subscription.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "subscriptions",
    indexes = {
        @Index(name = "idx_sub_recruiter", columnList = "recruiter_id"),
        @Index(name = "idx_sub_status",    columnList = "status"),
        @Index(name = "idx_sub_plan",      columnList = "plan")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subscriptionId;

    @Column(name = "recruiter_id", nullable = false)
    private Long recruiterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionPlan plan;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "amount_paid", nullable = false)
    private Double amountPaid;

    @Column(name = "max_job_posts")
    private Integer maxJobPosts;

    @Column(name = "auto_renew")
    @Builder.Default
    private Boolean autoRenew = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE
                && (endDate == null || !LocalDate.now().isAfter(endDate));
    }
}
