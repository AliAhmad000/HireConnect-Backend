package com.hireconnect.notification.service;

import com.hireconnect.notification.dto.request.SendNotificationRequest;
import com.hireconnect.notification.dto.response.NotificationResponse;
import com.hireconnect.notification.dto.response.UnreadCountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    NotificationResponse sendNotification(SendNotificationRequest request);

    NotificationResponse sendNotification(Long userId, String userEmail, String type,
                                          String title, String message,
                                          Long referenceId, String referenceType, String actionUrl);

    void sendEmailAlert(String toEmail, String subject, String body);

    Page<NotificationResponse> getNotificationsByUser(Long userId, Pageable pageable);

    List<NotificationResponse> getUnreadNotificationsByUser(Long userId);

    Page<NotificationResponse> getUnreadNotificationsByUserPaged(Long userId, Pageable pageable);

    UnreadCountResponse getUnreadCount(Long userId);

    NotificationResponse markAsRead(Long notificationId, Long userId);

    int markAllAsRead(Long userId);

    void deleteNotification(Long notificationId, Long userId);
}
