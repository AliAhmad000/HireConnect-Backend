package com.hireconnect.subscription.repository;

import com.hireconnect.subscription.entity.Subscription;
import com.hireconnect.subscription.enums.SubscriptionPlan;
import com.hireconnect.subscription.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByRecruiterId(Long recruiterId);

    List<Subscription> findByStatus(SubscriptionStatus status);

    Optional<Subscription> findFirstByRecruiterIdAndStatusOrderByCreatedAtDesc(Long recruiterId, SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s WHERE s.recruiterId = :rid AND s.status = 'ACTIVE' AND (s.endDate IS NULL OR s.endDate >= :today)")
    Optional<Subscription> findActiveByRecruiterId(@Param("rid") Long recruiterId, @Param("today") LocalDate today);

    long countByPlan(SubscriptionPlan plan);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate < :date AND s.autoRenew = false")
    List<Subscription> findExpiredSubscriptions(@Param("date") LocalDate date);
}
