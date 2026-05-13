package com.hireconnect.notification.event;

import com.hireconnect.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final RestTemplate restTemplate;

    @Value("${app.services.auth-url:http://localhost:8081/api/v1/auth}")
    private String authServiceUrl;

    @RabbitListener(queues = "notification.queue")
    public void handleNotificationEvent(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            log.info("Received event: {}", eventType);

            switch (eventType) {
                case "USER_REGISTERED"             -> handleUserRegistered(event);
                case "APPLICATION_SUBMITTED"      -> handleApplicationSubmitted(event);
                case "APPLICATION_STATUS_CHANGED" -> handleApplicationStatusChanged(event);
                case "APPLICATION_WITHDRAWN"      -> handleApplicationWithdrawn(event);
                case "INTERVIEW_SCHEDULED"        -> handleInterviewScheduled(event);
                case "INTERVIEW_CONFIRMED"        -> handleInterviewConfirmed(event);
                case "INTERVIEW_RESCHEDULE_REQUESTED" -> handleInterviewRescheduleRequested(event);
                case "INTERVIEW_RESCHEDULED"      -> handleInterviewRescheduled(event);
                case "INTERVIEW_RESCHEDULE_REJECTED" -> handleInterviewRescheduleRejected(event);
                case "INTERVIEW_CANCELLED"        -> handleInterviewCancelled(event);
                case "NEW_JOB_ALERT"              -> handleNewJobAlert(event);
                case "CANDIDATE_MESSAGE"          -> handleCandidateMessage(event);
                case "SUBSCRIPTION_PURCHASED"     -> handleSubscriptionPurchased(event);
                default -> log.warn("Unknown event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage(), e);
        }
    }

    private void handleUserRegistered(Map<String, Object> event) {
        Long userId = toLong(event.get("userId"));
        String email = (String) event.get("userEmail");
        String fullName = String.valueOf(event.getOrDefault("fullName", "there")).trim();
        String role = String.valueOf(event.getOrDefault("role", "")).trim();

        if (userId == null) {
            log.warn("USER_REGISTERED event missing userId");
            return;
        }

        String displayName = fullName.isBlank() ? "there" : fullName;
        String actionUrl = "RECRUITER".equalsIgnoreCase(role) ? "/recruiter/dashboard" : "/candidate/dashboard";
        String nextStep = "RECRUITER".equalsIgnoreCase(role)
                ? "You can now complete your company profile, post jobs, and manage applicants from your recruiter dashboard."
                : "You can now complete your profile, explore matching jobs, and track your applications from your candidate dashboard.";

        notificationService.sendNotification(
                userId, null,
                "USER_REGISTERED",
                "Welcome to HireConnect",
                "Hi " + displayName + ", welcome to HireConnect. " + nextStep,
                userId, "USER", actionUrl
        );
    }

    private void handleApplicationSubmitted(Map<String, Object> event) {
        Long candidateId  = toLong(event.get("candidateId"));
        String email      = (String) event.get("candidateEmail");
        String jobTitle   = (String) event.get("jobTitle");
        String company    = (String) event.get("companyName");
        Long appId        = toLong(event.get("applicationId"));
        String position   = positionText(jobTitle, company);

        // Notify candidate
        notificationService.sendNotification(
                candidateId, email,
                "APPLICATION_SUBMITTED",
                "Application Received",
                "Your application for " + position + " has been received. You will get a system email whenever it moves to the next hiring stage.",
                appId, "APPLICATION", "/candidate/applications/" + appId
        );

        // Notify recruiter
        Long recruiterId  = toLong(event.get("recruiterId"));
        String candidateName = (String) event.get("candidateName");
        if (recruiterId != null) {
            notificationService.sendNotification(
                    recruiterId, null,
                    "NEW_APPLICATION",
                    "New Application Received",
                    safeText(candidateName, "A candidate") + " has applied for " + position + ".",
                    appId, "APPLICATION", "/recruiter/applications/" + appId
            );
        }
    }

    private void handleApplicationStatusChanged(Map<String, Object> event) {
        Long candidateId = toLong(event.get("candidateId"));
        String email     = (String) event.get("candidateEmail");
        String jobTitle  = (String) event.get("jobTitle");
        String company   = (String) event.get("companyName");
        String newStatus = (String) event.get("newStatus");
        String reason    = (String) event.get("rejectionReason");
        Long appId       = toLong(event.get("applicationId"));

        String title   = buildStatusChangeTitle(newStatus);
        String message = buildStatusChangeMessage(jobTitle, company, newStatus, reason);

        notificationService.sendNotification(
                candidateId, email,
                "APPLICATION_STATUS_CHANGED",
                title, message,
                appId, "APPLICATION", "/candidate/applications/" + appId
        );
    }

    private void handleApplicationWithdrawn(Map<String, Object> event) {
        Long candidateId = toLong(event.get("candidateId"));
        String email     = (String) event.get("candidateEmail");
        Long recruiterId = toLong(event.get("recruiterId"));
        String jobTitle  = (String) event.get("jobTitle");
        String company   = (String) event.get("companyName");
        String cName     = (String) event.get("candidateName");
        Long appId       = toLong(event.get("applicationId"));
        String position  = positionText(jobTitle, company);

        if (candidateId != null) {
            notificationService.sendNotification(
                    candidateId, email,
                    "APPLICATION_WITHDRAWN",
                    "Application Withdrawn",
                    "Your application for " + position + " has been withdrawn successfully.",
                    appId, "APPLICATION", "/candidate/applications/" + appId
            );
        }

        if (recruiterId != null) {
            notificationService.sendNotification(
                    recruiterId, null,
                    "APPLICATION_WITHDRAWN",
                    "Application Withdrawn",
                    safeText(cName, "A candidate") + " has withdrawn their application for " + position + ".",
                    appId, "APPLICATION", "/recruiter/applications/" + appId
            );
        }
    }

    private void handleInterviewScheduled(Map<String, Object> event) {
        Long candidateId   = toLong(event.get("candidateId"));
        String email       = (String) event.get("candidateEmail");
        String jobTitle    = (String) event.get("jobTitle");
        String company     = (String) event.get("companyName");
        String scheduledAt = (String) event.get("scheduledAt");
        String mode        = (String) event.get("mode");
        Long interviewId   = toLong(event.get("interviewId"));

        notificationService.sendNotification(
                candidateId, email,
                "INTERVIEW_SCHEDULED",
                "Interview Scheduled — " + jobTitle,
                "Your interview for " + jobTitle + " at " + company +
                " is scheduled on " + scheduledAt + " (" + mode + "). Please confirm.",
                interviewId, "INTERVIEW", "/candidate/interviews"
        );
    }

    private void handleInterviewConfirmed(Map<String, Object> event) {
        Long recruiterId   = toLong(event.get("recruiterId"));
        String candidateName = (String) event.get("candidateName");
        String jobTitle    = (String) event.get("jobTitle");
        Long interviewId   = toLong(event.get("interviewId"));
        Long appId         = toLong(event.get("applicationId"));

        if (recruiterId != null) {
            notificationService.sendNotification(
                    recruiterId, null,
                    "INTERVIEW_CONFIRMED",
                    "Interview Confirmed",
                    candidateName + " has confirmed the interview for " + jobTitle + ".",
                    interviewId, "INTERVIEW", recruiterApplicationUrl(appId)
            );
        }
    }

    private void handleInterviewRescheduled(Map<String, Object> event) {
        Long candidateId   = toLong(event.get("candidateId"));
        String email       = (String) event.get("candidateEmail");
        Long recruiterId   = toLong(event.get("recruiterId"));
        String jobTitle    = (String) event.get("jobTitle");
        String scheduledAt = (String) event.get("scheduledAt");
        Long interviewId   = toLong(event.get("interviewId"));
        Long appId         = toLong(event.get("applicationId"));

        notificationService.sendNotification(
                candidateId, email,
                "INTERVIEW_RESCHEDULED",
                "Interview Rescheduled — " + jobTitle,
                "Your interview for " + jobTitle + " has been rescheduled to " + scheduledAt + ".",
                interviewId, "INTERVIEW", "/candidate/interviews"
        );
        if (recruiterId != null) {
            notificationService.sendNotification(
                    recruiterId, null,
                    "INTERVIEW_RESCHEDULED",
                    "Interview Rescheduled",
                    "Interview for " + jobTitle + " has been rescheduled to " + scheduledAt + ".",
                    interviewId, "INTERVIEW", recruiterApplicationUrl(appId)
            );
        }
    }

    private void handleInterviewRescheduleRequested(Map<String, Object> event) {
        Long recruiterId = toLong(event.get("recruiterId"));
        String candidateName = (String) event.get("candidateName");
        String jobTitle = (String) event.get("jobTitle");
        String requestedAt = (String) event.get("requestedScheduledAt");
        Long interviewId = toLong(event.get("interviewId"));
        Long appId = toLong(event.get("applicationId"));

        if (recruiterId != null) {
            notificationService.sendNotification(
                    recruiterId, null,
                    "INTERVIEW_RESCHEDULE_REQUESTED",
                    "Interview Reschedule Requested",
                    candidateName + " requested a new interview time for " + jobTitle + ": " + requestedAt + ".",
                    interviewId, "INTERVIEW", recruiterApplicationUrl(appId)
            );
        }
    }

    private void handleInterviewRescheduleRejected(Map<String, Object> event) {
        Long candidateId = toLong(event.get("candidateId"));
        String email = (String) event.get("candidateEmail");
        String jobTitle = (String) event.get("jobTitle");
        String scheduledAt = (String) event.get("scheduledAt");
        Long interviewId = toLong(event.get("interviewId"));

        notificationService.sendNotification(
                candidateId, email,
                "INTERVIEW_RESCHEDULE_REJECTED",
                "Reschedule Request Declined",
                "Your reschedule request for " + jobTitle + " was declined. The interview remains scheduled for " + scheduledAt + ".",
                interviewId, "INTERVIEW", "/candidate/interviews"
        );
    }

    private void handleInterviewCancelled(Map<String, Object> event) {
        Long candidateId = toLong(event.get("candidateId"));
        String email     = (String) event.get("candidateEmail");
        Long recruiterId = toLong(event.get("recruiterId"));
        String jobTitle  = (String) event.get("jobTitle");
        Long interviewId = toLong(event.get("interviewId"));
        Long appId       = toLong(event.get("applicationId"));

        notificationService.sendNotification(
                candidateId, email,
                "INTERVIEW_CANCELLED",
                "Interview Cancelled — " + jobTitle,
                "Your interview for " + jobTitle + " has been cancelled.",
                interviewId, "INTERVIEW", "/candidate/interviews"
        );
        if (recruiterId != null) {
            notificationService.sendNotification(
                    recruiterId, null,
                    "INTERVIEW_CANCELLED",
                    "Interview Cancelled",
                    "The interview for " + jobTitle + " has been cancelled.",
                    interviewId, "INTERVIEW", recruiterApplicationUrl(appId)
            );
        }
    }

    private void handleNewJobAlert(Map<String, Object> event) {
        Long candidateId = toLong(event.get("candidateId"));
        String email = (String) event.get("candidateEmail");
        String jobTitle = (String) event.get("jobTitle");
        String company = (String) event.get("companyName");
        String location = (String) event.get("location");
        Boolean isRemote = (Boolean) event.get("isRemote");
        Long jobId = toLong(event.get("jobId"));

        String workplace = Boolean.TRUE.equals(isRemote)
                ? "Remote"
                : (location != null && !location.isBlank() ? location : "Location not specified");
        String companyText = company != null && !company.isBlank() ? " at " + company : "";

        notificationService.sendNotification(
                candidateId, email,
                "NEW_JOB_ALERT",
                "New Job Alert — " + jobTitle,
                "A new job matching your interests is available: " + jobTitle + companyText + " (" + workplace + ").",
                jobId, "JOB", "/jobs/" + jobId
        );
    }

    private void handleCandidateMessage(Map<String, Object> event) {
        Long candidateId = toLong(event.get("candidateId"));
        String email = (String) event.get("candidateEmail");
        String jobTitle = (String) event.get("jobTitle");
        String company = (String) event.get("companyName");
        String message = (String) event.get("message");
        Long appId = toLong(event.get("applicationId"));

        String companyText = company != null && !company.isBlank() ? " at " + company : "";
        notificationService.sendNotification(
                candidateId, email,
                "CANDIDATE_MESSAGE",
                "Message from recruiter - " + jobTitle,
                "Regarding your shortlisted application for " + jobTitle + companyText + ": " + message,
                appId, "APPLICATION", "/candidate/applications/" + appId
        );
    }

    private void handleSubscriptionPurchased(Map<String, Object> event) {
        List<Long> adminIds = toLongList(event.get("adminIds"));
        Long recruiterId = toLong(event.get("recruiterId"));
        Long subscriptionId = toLong(event.get("subscriptionId"));
        String plan = String.valueOf(event.getOrDefault("plan", "subscription"));
        String amount = String.valueOf(event.getOrDefault("amount", "0"));
        String paymentMode = String.valueOf(event.getOrDefault("paymentMode", ""));
        String invoiceNumber = String.valueOf(event.getOrDefault("invoiceNumber", ""));

        if (adminIds.isEmpty()) {
            adminIds = fetchActiveAdminIds();
        }

        if (adminIds.isEmpty()) {
            log.warn("SUBSCRIPTION_PURCHASED event has no admin recipients");
            return;
        }

        String paymentText = paymentMode.isBlank() ? "" : " via " + paymentMode.replace('_', ' ');
        String invoiceText = invoiceNumber.isBlank() ? "" : " Invoice: " + invoiceNumber + ".";
        String message = "Recruiter #" + recruiterId + " purchased the " + plan +
                " plan for INR " + amount + paymentText + "." + invoiceText;

        for (Long adminId : adminIds) {
            notificationService.sendNotification(
                    adminId, null,
                    "SUBSCRIPTION_PURCHASED",
                    "New Subscription Purchased",
                    message,
                    subscriptionId, "SUBSCRIPTION", "/admin/subscriptions"
            );
        }
    }

    // ---------- helpers ----------

    private String buildStatusChangeTitle(String status) {
        return switch (safeText(status, "").toUpperCase()) {
            case "SHORTLISTED"          -> "Application Shortlisted";
            case "INTERVIEW_SCHEDULED"  -> "Interview Stage Started";
            case "OFFERED"              -> "Offer Received";
            case "REJECTED"             -> "Application Update";
            case "WITHDRAWN"            -> "Application Withdrawn";
            default                     -> "Application Status Updated";
        };
    }

    private String buildStatusChangeMessage(String jobTitle, String company, String status, String rejectionReason) {
        String position = positionText(jobTitle, company);
        String normalizedStatus = safeText(status, "").toUpperCase();
        return switch (normalizedStatus) {
            case "SHORTLISTED"          ->
                "Good news! Your application for " + position + " has been shortlisted. The recruiter may contact you for the next step.";
            case "INTERVIEW_SCHEDULED"  ->
                "Your application for " + position + " has moved to the interview stage. Please check My Interviews for schedule details.";
            case "OFFERED"              ->
                "Congratulations! Your application for " + position + " has moved to offered. Please check your dashboard for the next steps.";
            case "REJECTED"             ->
                "Thank you for applying for " + position + ". Your application has not moved forward at this time." + rejectionReasonText(rejectionReason);
            case "WITHDRAWN"            ->
                "Your application for " + position + " has been withdrawn successfully.";
            default                     ->
                "Your application for " + position + " has been updated to " + statusLabel(status) + ".";
        };
    }

    private String positionText(String jobTitle, String company) {
        String title = safeText(jobTitle, "");
        String companyName = safeText(company, "");
        if (title.isBlank() && companyName.isBlank()) {
            return "this role";
        }
        if (title.isBlank()) {
            return "a role at " + companyName;
        }
        if (companyName.isBlank()) {
            return title;
        }
        return title + " at " + companyName;
    }

    private String rejectionReasonText(String rejectionReason) {
        String reason = safeText(rejectionReason, "");
        return reason.isBlank() ? "" : " Reason: " + reason;
    }

    private String statusLabel(String status) {
        return safeText(status, "updated").replace('_', ' ').toLowerCase();
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim())) {
            return fallback;
        }
        return value.trim();
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(value.toString()); } catch (Exception e) { return null; }
    }

    private List<Long> toLongList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return values.stream()
                .map(this::toLong)
                .filter(id -> id != null)
                .toList();
    }

    private List<Long> fetchActiveAdminIds() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(authServiceUrl + "/internal/admin-ids", Map.class);
            Object data = response.getBody() != null ? response.getBody().get("data") : null;
            return toLongList(data);
        } catch (Exception e) {
            log.warn("Unable to fetch active admin ids for notification event: {}", e.getMessage());
            return List.of();
        }
    }

    private String recruiterApplicationUrl(Long applicationId) {
        return applicationId != null
                ? "/recruiter/applications/" + applicationId
                : "/recruiter/applications";
    }
}
