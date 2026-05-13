package com.hireconnect.notification.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long notificationId;
    private Long userId;
    private String type;
    private String title;
    private String message;
    private Boolean isRead;
    private Long referenceId;
    private String referenceType;
    private String actionUrl;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
