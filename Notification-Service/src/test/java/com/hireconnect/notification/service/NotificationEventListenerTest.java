package com.hireconnect.notification.service;

import com.hireconnect.notification.event.NotificationEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener Tests")
class NotificationEventListenerTest {

    @Mock private NotificationService notificationService;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private NotificationEventListener eventListener;

    private Map<String, Object> buildEvent(String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("applicationId", 100L);
        event.put("candidateId", 1L);
        event.put("candidateEmail", "alice@example.com");
        event.put("candidateName", "Alice Smith");
        event.put("jobId", 10L);
        event.put("jobTitle", "Software Engineer");
        event.put("companyName", "TechCorp");
        event.put("recruiterId", 2L);
        event.put("interviewId", 200L);
        event.put("scheduledAt", "2026-06-01T10:00:00");
        event.put("mode", "ONLINE");
        event.put("status", "SCHEDULED");
        return event;
    }

    @Test
    @DisplayName("USER_REGISTERED should send welcome notification and email")
    void shouldSendWelcomeNotificationOnUserRegistered() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "USER_REGISTERED");
        event.put("userId", 1L);
        event.put("userEmail", "alice@example.com");
        event.put("fullName", "Alice Smith");
        event.put("role", "CANDIDATE");

        eventListener.handleNotificationEvent(event);

        verify(notificationService).sendNotification(
                eq(1L), isNull(),
                eq("USER_REGISTERED"),
                eq("Welcome to HireConnect"),
                contains("Alice Smith"),
                eq(1L), eq("USER"), eq("/candidate/dashboard"));
    }

    @Test
    @DisplayName("APPLICATION_SUBMITTED should notify both candidate and recruiter")
    void shouldNotifyBothOnApplicationSubmitted() {
        eventListener.handleNotificationEvent(buildEvent("APPLICATION_SUBMITTED"));

        // Candidate notification
        verify(notificationService).sendNotification(
                eq(1L), eq("alice@example.com"),
                eq("APPLICATION_SUBMITTED"),
                anyString(), anyString(),
                eq(100L), eq("APPLICATION"), anyString());

        // Recruiter notification
        verify(notificationService).sendNotification(
                eq(2L), isNull(),
                eq("NEW_APPLICATION"),
                anyString(), anyString(),
                eq(100L), eq("APPLICATION"), anyString());
    }

    @Test
    @DisplayName("APPLICATION_STATUS_CHANGED should notify candidate")
    void shouldNotifyCandidateOnStatusChange() {
        Map<String, Object> event = buildEvent("APPLICATION_STATUS_CHANGED");
        event.put("oldStatus", "APPLIED");
        event.put("newStatus", "SHORTLISTED");

        eventListener.handleNotificationEvent(event);

        verify(notificationService).sendNotification(
                eq(1L), eq("alice@example.com"),
                eq("APPLICATION_STATUS_CHANGED"),
                contains("Shortlisted"),
                contains("shortlisted"),
                eq(100L), eq("APPLICATION"), anyString());
    }

    @Test
    @DisplayName("APPLICATION_STATUS_CHANGED should include rejection reason when rejected")
    void shouldNotifyCandidateWithRejectionReason() {
        Map<String, Object> event = buildEvent("APPLICATION_STATUS_CHANGED");
        event.put("oldStatus", "INTERVIEW_SCHEDULED");
        event.put("newStatus", "REJECTED");
        event.put("rejectionReason", "The role was closed.");

        eventListener.handleNotificationEvent(event);

        verify(notificationService).sendNotification(
                eq(1L), eq("alice@example.com"),
                eq("APPLICATION_STATUS_CHANGED"),
                eq("Application Update"),
                contains("The role was closed."),
                eq(100L), eq("APPLICATION"), anyString());
    }

    @Test
    @DisplayName("APPLICATION_WITHDRAWN should notify candidate and recruiter")
    void shouldNotifyCandidateAndRecruiterOnApplicationWithdrawn() {
        eventListener.handleNotificationEvent(buildEvent("APPLICATION_WITHDRAWN"));

        verify(notificationService).sendNotification(
                eq(1L), eq("alice@example.com"),
                eq("APPLICATION_WITHDRAWN"),
                eq("Application Withdrawn"),
                contains("withdrawn successfully"),
                eq(100L), eq("APPLICATION"), eq("/candidate/applications/100"));
        verify(notificationService).sendNotification(
                eq(2L), isNull(),
                eq("APPLICATION_WITHDRAWN"),
                eq("Application Withdrawn"),
                contains("Alice Smith has withdrawn"),
                eq(100L), eq("APPLICATION"), eq("/recruiter/applications/100"));
    }

    @Test
    @DisplayName("INTERVIEW_SCHEDULED should notify candidate")
    void shouldNotifyCandidateOnInterviewScheduled() {
        eventListener.handleNotificationEvent(buildEvent("INTERVIEW_SCHEDULED"));

        verify(notificationService).sendNotification(
                eq(1L), eq("alice@example.com"),
                eq("INTERVIEW_SCHEDULED"),
                anyString(), anyString(),
                eq(200L), eq("INTERVIEW"), anyString());
    }

    @Test
    @DisplayName("INTERVIEW_RESCHEDULE_REQUESTED should notify recruiter")
    void shouldNotifyRecruiterOnRescheduleRequest() {
        Map<String, Object> event = buildEvent("INTERVIEW_RESCHEDULE_REQUESTED");
        event.put("requestedScheduledAt", "2026-06-02T10:00:00");

        eventListener.handleNotificationEvent(event);

        verify(notificationService).sendNotification(
                eq(2L), isNull(),
                eq("INTERVIEW_RESCHEDULE_REQUESTED"),
                anyString(), contains("requested"),
                eq(200L), eq("INTERVIEW"), anyString());
    }

    @Test
    @DisplayName("INTERVIEW_RESCHEDULE_REJECTED should notify candidate")
    void shouldNotifyCandidateOnRescheduleRejected() {
        eventListener.handleNotificationEvent(buildEvent("INTERVIEW_RESCHEDULE_REJECTED"));

        verify(notificationService).sendNotification(
                eq(1L), eq("alice@example.com"),
                eq("INTERVIEW_RESCHEDULE_REJECTED"),
                anyString(), contains("declined"),
                eq(200L), eq("INTERVIEW"), anyString());
    }

    @Test
    @DisplayName("INTERVIEW_CONFIRMED should notify recruiter")
    void shouldNotifyRecruiterOnInterviewConfirmed() {
        eventListener.handleNotificationEvent(buildEvent("INTERVIEW_CONFIRMED"));

        verify(notificationService).sendNotification(
                eq(2L), isNull(),
                eq("INTERVIEW_CONFIRMED"),
                anyString(), anyString(),
                eq(200L), eq("INTERVIEW"), anyString());
    }

    @Test
    @DisplayName("INTERVIEW_RESCHEDULED should notify both candidate and recruiter")
    void shouldNotifyBothOnInterviewRescheduled() {
        eventListener.handleNotificationEvent(buildEvent("INTERVIEW_RESCHEDULED"));

        // Candidate
        verify(notificationService).sendNotification(
                eq(1L), eq("alice@example.com"),
                eq("INTERVIEW_RESCHEDULED"),
                anyString(), anyString(),
                eq(200L), eq("INTERVIEW"), anyString());

        // Recruiter
        verify(notificationService).sendNotification(
                eq(2L), isNull(),
                eq("INTERVIEW_RESCHEDULED"),
                anyString(), anyString(),
                eq(200L), eq("INTERVIEW"), anyString());
    }

    @Test
    @DisplayName("INTERVIEW_CANCELLED should notify both candidate and recruiter")
    void shouldNotifyBothOnInterviewCancelled() {
        eventListener.handleNotificationEvent(buildEvent("INTERVIEW_CANCELLED"));

        verify(notificationService, times(2)).sendNotification(
                anyLong(), any(), eq("INTERVIEW_CANCELLED"),
                anyString(), anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("SUBSCRIPTION_PURCHASED should notify every admin in the event")
    void shouldNotifyAdminsOnSubscriptionPurchased() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "SUBSCRIPTION_PURCHASED");
        event.put("adminIds", List.of(9L, 10L));
        event.put("recruiterId", 2L);
        event.put("subscriptionId", 44L);
        event.put("plan", "PROFESSIONAL");
        event.put("amount", 2358.82);
        event.put("paymentMode", "UPI");
        event.put("invoiceNumber", "HC-INV-20260502-TEST1234");

        eventListener.handleNotificationEvent(event);

        verify(notificationService).sendNotification(
                eq(9L), isNull(),
                eq("SUBSCRIPTION_PURCHASED"),
                eq("New Subscription Purchased"),
                contains("PROFESSIONAL"),
                eq(44L), eq("SUBSCRIPTION"), eq("/admin/subscriptions"));
        verify(notificationService).sendNotification(
                eq(10L), isNull(),
                eq("SUBSCRIPTION_PURCHASED"),
                eq("New Subscription Purchased"),
                contains("PROFESSIONAL"),
                eq(44L), eq("SUBSCRIPTION"), eq("/admin/subscriptions"));
    }

    @Test
    @DisplayName("SUBSCRIPTION_PURCHASED should fetch admins when event has none")
    void shouldFetchAdminsWhenSubscriptionEventHasNoRecipients() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "SUBSCRIPTION_PURCHASED");
        event.put("recruiterId", "2");
        event.put("subscriptionId", 44);
        event.put("plan", "ENTERPRISE");
        event.put("amount", "5898.82");

        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("data", List.of(11, "12"))));

        eventListener.handleNotificationEvent(event);

        verify(notificationService).sendNotification(eq(11L), isNull(), eq("SUBSCRIPTION_PURCHASED"),
                anyString(), contains("ENTERPRISE"), eq(44L), eq("SUBSCRIPTION"), anyString());
        verify(notificationService).sendNotification(eq(12L), isNull(), eq("SUBSCRIPTION_PURCHASED"),
                anyString(), contains("ENTERPRISE"), eq(44L), eq("SUBSCRIPTION"), anyString());
    }

    @Test
    @DisplayName("NEW_JOB_ALERT should notify candidate")
    void shouldNotifyCandidateForNewJobAlert() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "NEW_JOB_ALERT");
        event.put("candidateId", 1L);
        event.put("candidateEmail", "alice@example.com");
        event.put("jobId", 10L);
        event.put("jobTitle", "Backend Developer");
        event.put("companyName", "TechCorp");

        eventListener.handleNotificationEvent(event);

        verify(notificationService).sendNotification(eq(1L), eq("alice@example.com"), eq("NEW_JOB_ALERT"),
                anyString(), contains("Backend Developer"), eq(10L), eq("JOB"), anyString());
    }

    @Test
    @DisplayName("CANDIDATE_MESSAGE should notify candidate")
    void shouldNotifyCandidateForRecruiterMessage() {
        Map<String, Object> event = buildEvent("CANDIDATE_MESSAGE");
        event.put("message", "Please share your portfolio.");

        eventListener.handleNotificationEvent(event);

        verify(notificationService).sendNotification(eq(1L), eq("alice@example.com"), eq("CANDIDATE_MESSAGE"),
                contains("Software Engineer"), contains("portfolio"), eq(100L), eq("APPLICATION"), anyString());
    }

    @Test
    @DisplayName("Unknown event type should be silently ignored")
    void shouldIgnoreUnknownEventType() {
        eventListener.handleNotificationEvent(buildEvent("UNKNOWN_EVENT_TYPE"));

        verify(notificationService, never()).sendNotification(
                anyLong(), any(), anyString(), anyString(), anyString(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Null event fields should be handled gracefully without exception")
    void shouldHandleNullEventFieldsGracefully() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "APPLICATION_SUBMITTED");
        event.put("candidateId", 1L);
        // Missing candidateEmail, recruiterId etc.

        // Should not throw even with partial data
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                eventListener.handleNotificationEvent(event));
    }
}
