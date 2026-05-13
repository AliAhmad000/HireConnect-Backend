package com.hireconnect.analytics.event;

import com.hireconnect.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventListener {

    private final AnalyticsService analyticsService;

    @RabbitListener(queues = "analytics.queue")
    public void handleAnalyticsEvent(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            log.info("Analytics received event: {}", eventType);

            switch (eventType) {
                case "APPLICATION_SUBMITTED" -> {
                    analyticsService.recordApplicationEvent(
                            toLong(event.get("applicationId")),
                            toLong(event.get("jobId")),
                            toLong(event.get("recruiterId")),
                            toLong(event.get("candidateId")),
                            "APPLICATION_SUBMITTED",
                            null,
                            "APPLIED"
                    );
                }
                case "APPLICATION_STATUS_CHANGED" -> {
                    analyticsService.recordApplicationEvent(
                            toLong(event.get("applicationId")),
                            toLong(event.get("jobId")),
                            toLong(event.get("recruiterId")),
                            toLong(event.get("candidateId")),
                            "APPLICATION_STATUS_CHANGED",
                            (String) event.get("oldStatus"),
                            (String) event.get("newStatus")
                    );
                }
                case "JOB_VIEWED" -> {
                    analyticsService.recordJobView(
                            toLong(event.get("jobId")),
                            toLong(event.get("recruiterId")),
                            toLong(event.get("viewerId")),
                            (String) event.get("viewerRole")
                    );
                }
                default -> log.debug("Unhandled analytics event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing analytics event: {}", e.getMessage(), e);
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(value.toString()); } catch (Exception e) { return null; }
    }
}
