package com.hireconnect.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class SendNotificationRequest {

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    @Schema(example = "21")
    private Long userId;

    @Schema(example = "aarav.sharma@example.com")
    private String userEmail;

    @NotBlank(message = "Notification type is required")
    @Schema(example = "INTERVIEW_SCHEDULED")
    private String type;

    @NotBlank(message = "Title is required")
    @Schema(example = "Interview scheduled")
    private String title;

    @NotBlank(message = "Message is required")
    @Schema(example = "Your interview for Senior Java Developer is scheduled on 10 May 2026 at 10:30 AM.")
    private String message;

    @Schema(example = "501")
    private Long referenceId;
    @Schema(example = "INTERVIEW")
    private String referenceType;
    @Schema(example = "/candidate/interviews/501")
    private String actionUrl;
    @Schema(example = "true")
    private boolean sendEmail = false;
}
