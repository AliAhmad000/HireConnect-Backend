package com.hireconnect.subscription.config;

import com.hireconnect.subscription.entity.Subscription;
import com.hireconnect.subscription.enums.SubscriptionStatus;
import com.hireconnect.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryScheduler {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * Runs every day at midnight — marks all lapsed ACTIVE subscriptions as EXPIRED.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expireSubscriptions() {
        List<Subscription> expired =
                subscriptionRepository.findExpiredSubscriptions(LocalDate.now());

        if (expired.isEmpty()) {
            log.debug("No subscriptions to expire today.");
            return;
        }

        expired.forEach(s -> {
            s.setStatus(SubscriptionStatus.EXPIRED);
            log.info("Subscription {} (recruiter {}) expired on {}",
                    s.getSubscriptionId(), s.getRecruiterId(), s.getEndDate());
        });

        subscriptionRepository.saveAll(expired);
        log.info("Expired {} subscription(s)", expired.size());
    }
}
