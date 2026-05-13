package com.hireconnect.notification.service.impl;

import com.hireconnect.notification.dto.request.SendNotificationRequest;
import com.hireconnect.notification.dto.response.NotificationResponse;
import com.hireconnect.notification.dto.response.UnreadCountResponse;
import com.hireconnect.notification.entity.Notification;
import com.hireconnect.notification.exception.ResourceNotFoundException;
import com.hireconnect.notification.mapper.NotificationMapper;
import com.hireconnect.notification.repository.NotificationRepository;
import com.hireconnect.notification.service.NotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Override
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        return sendNotification(
                request.getUserId(),
                request.getUserEmail(),
                request.getType(),
                request.getTitle(),
                request.getMessage(),
                request.getReferenceId(),
                request.getReferenceType(),
                request.getActionUrl()
        );
    }

    @Override
    public NotificationResponse sendNotification(Long userId, String userEmail, String type,
                                                  String title, String message,
                                                  Long referenceId, String referenceType, String actionUrl) {
        log.info("Sending in-app notification to user {}: type={}", userId, type);

        Notification notification = Notification.builder()
                .userId(userId)
                .userEmail(userEmail)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .actionUrl(actionUrl)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.debug("Notification {} saved for user {}", saved.getNotificationId(), userId);

        // Also send email if address available
        if (userEmail != null && !userEmail.isBlank()) {
            sendEmailAlert(userEmail, title, message);
        }

        return notificationMapper.toResponse(saved);
    }

    @Override
    @Async
    public void sendEmailAlert(String toEmail, String subject, String body) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(buildHtmlEmail(subject, body), true);
            mailSender.send(mimeMessage);
            log.info("Email sent to {}: subject={}", toEmail, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsByUser(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotificationsByUser(Long userId) {
        return notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false)
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUnreadNotificationsByUserPaged(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndIsRead(userId, false, pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long userId) {
        long count = notificationRepository.countByUserIdAndIsRead(userId, false);
        return new UnreadCountResponse(userId, count);
    }

    @Override
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        int updated = notificationRepository.markAsRead(notificationId, userId);
        if (updated == 0) {
            throw new ResourceNotFoundException("Notification not found with ID: " + notificationId);
        }
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        return notificationMapper.toResponse(notification);
    }

    @Override
    public int markAllAsRead(Long userId) {
        int count = notificationRepository.markAllAsRead(userId);
        log.info("Marked {} notifications as read for user {}", count, userId);
        return count;
    }

    @Override
    public void deleteNotification(Long notificationId, Long userId) {
        int deleted = notificationRepository.deleteByNotificationIdAndUserId(notificationId, userId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Notification not found with ID: " + notificationId);
        }
    }

    private String buildHtmlEmail(String subject, String body) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <style>
                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px;
                                 padding: 30px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { background-color: #2563eb; color: white; padding: 20px; border-radius: 6px 6px 0 0;
                              text-align: center; margin: -30px -30px 20px -30px; }
                    .content { color: #333; line-height: 1.6; }
                    .footer { text-align: center; color: #888; font-size: 12px; margin-top: 30px;
                              padding-top: 20px; border-top: 1px solid #eee; }
                  </style>
                </head>
                <body>
                  <div class="container">
                    <div class="header"><h2>HireConnect</h2><p>Bridging Talent with Opportunity</p></div>
                    <div class="content">
                      <h3>%s</h3>
                      <p>%s</p>
                    </div>
                    <div class="footer">
                      <p>You're receiving this because you have an account on HireConnect.</p>
                      <p>&copy; 2026 HireConnect. All rights reserved.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(subject, body);
    }
}
