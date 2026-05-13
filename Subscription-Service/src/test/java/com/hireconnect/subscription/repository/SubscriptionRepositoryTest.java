package com.hireconnect.subscription.repository;

import com.hireconnect.subscription.entity.Subscription;
import com.hireconnect.subscription.enums.SubscriptionPlan;
import com.hireconnect.subscription.enums.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SubscriptionRepository Tests")
class SubscriptionRepositoryTest {

    @Autowired private SubscriptionRepository subscriptionRepository;

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();
    }

    private Subscription buildSub(Long recruiterId, SubscriptionPlan plan,
                                   SubscriptionStatus status, LocalDate endDate) {
        return Subscription.builder()
                .recruiterId(recruiterId)
                .plan(plan)
                .startDate(LocalDate.now())
                .endDate(endDate)
                .status(status)
                .amountPaid(plan == SubscriptionPlan.PROFESSIONAL ? 1999.0 : 0.0)
                .maxJobPosts(plan == SubscriptionPlan.PROFESSIONAL ? 50 : 3)
                .autoRenew(false)
                .build();
    }

    @Test
    @DisplayName("findActiveByRecruiterId() should return active non-expired subscription")
    void shouldFindActiveSubscription() {
        subscriptionRepository.save(buildSub(1L, SubscriptionPlan.PROFESSIONAL,
                SubscriptionStatus.ACTIVE, LocalDate.now().plusDays(20)));

        Optional<Subscription> found = subscriptionRepository
                .findActiveByRecruiterId(1L, LocalDate.now());

        assertThat(found).isPresent();
        assertThat(found.get().getPlan()).isEqualTo(SubscriptionPlan.PROFESSIONAL);
    }

    @Test
    @DisplayName("findActiveByRecruiterId() should not return expired subscription")
    void shouldNotReturnExpiredSubscription() {
        subscriptionRepository.save(buildSub(1L, SubscriptionPlan.PROFESSIONAL,
                SubscriptionStatus.ACTIVE, LocalDate.now().minusDays(1))); // expired yesterday

        Optional<Subscription> found = subscriptionRepository
                .findActiveByRecruiterId(1L, LocalDate.now());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findActiveByRecruiterId() should not return CANCELLED subscription")
    void shouldNotReturnCancelledSubscription() {
        subscriptionRepository.save(buildSub(1L, SubscriptionPlan.PROFESSIONAL,
                SubscriptionStatus.CANCELLED, LocalDate.now().plusDays(10)));

        Optional<Subscription> found = subscriptionRepository
                .findActiveByRecruiterId(1L, LocalDate.now());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByRecruiterId() should return all subscriptions for recruiter")
    void shouldReturnAllSubscriptionsForRecruiter() {
        subscriptionRepository.save(buildSub(1L, SubscriptionPlan.FREE,
                SubscriptionStatus.CANCELLED, null));
        subscriptionRepository.save(buildSub(1L, SubscriptionPlan.PROFESSIONAL,
                SubscriptionStatus.ACTIVE, LocalDate.now().plusDays(30)));

        List<Subscription> all = subscriptionRepository.findByRecruiterId(1L);

        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("findExpiredSubscriptions() should return subscriptions past endDate")
    void shouldFindExpiredSubscriptions() {
        // Active but past end date
        subscriptionRepository.save(buildSub(1L, SubscriptionPlan.PROFESSIONAL,
                SubscriptionStatus.ACTIVE, LocalDate.now().minusDays(1)));

        // Active and still valid
        subscriptionRepository.save(buildSub(2L, SubscriptionPlan.PROFESSIONAL,
                SubscriptionStatus.ACTIVE, LocalDate.now().plusDays(10)));

        List<Subscription> expired = subscriptionRepository
                .findExpiredSubscriptions(LocalDate.now());

        assertThat(expired).hasSize(1);
        assertThat(expired.get(0).getRecruiterId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("countByPlan() should count subscriptions by plan type")
    void shouldCountByPlan() {
        subscriptionRepository.save(buildSub(1L, SubscriptionPlan.PROFESSIONAL,
                SubscriptionStatus.ACTIVE, LocalDate.now().plusDays(30)));
        subscriptionRepository.save(buildSub(2L, SubscriptionPlan.PROFESSIONAL,
                SubscriptionStatus.ACTIVE, LocalDate.now().plusDays(30)));
        subscriptionRepository.save(buildSub(3L, SubscriptionPlan.ENTERPRISE,
                SubscriptionStatus.ACTIVE, LocalDate.now().plusDays(30)));

        assertThat(subscriptionRepository.countByPlan(SubscriptionPlan.PROFESSIONAL)).isEqualTo(2L);
        assertThat(subscriptionRepository.countByPlan(SubscriptionPlan.ENTERPRISE)).isEqualTo(1L);
        assertThat(subscriptionRepository.countByPlan(SubscriptionPlan.FREE)).isEqualTo(0L);
    }
}
