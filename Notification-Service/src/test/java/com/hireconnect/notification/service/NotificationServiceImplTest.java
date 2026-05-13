package com.hireconnect.notification.service;

import com.hireconnect.notification.dto.response.NotificationResponse;
import com.hireconnect.notification.dto.response.UnreadCountResponse;
import com.hireconnect.notification.entity.Notification;
import com.hireconnect.notification.exception.ResourceNotFoundException;
import com.hireconnect.notification.mapper.NotificationMapper;
import com.hireconnect.notification.repository.NotificationRepository;
import com.hireconnect.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Tests")
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationMapper notificationMapper;
    @Mock private JavaMailSender mailSender;

    @InjectMocks private NotificationServiceImpl notificationService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "fromEmail", "noreply@hireconnect.com");
        ReflectionTestUtils.setField(notificationService, "fromName", "HireConnect");
    }

    private Notification buildNotification(Long id, boolean isRead) {
        return Notification.builder()
                .notificationId(id)
                .userId(1L)
                .userEmail("user@example.com")
                .type("APPLICATION_SUBMITTED")
                .title("Application Submitted")
                .message("Your application has been submitted.")
                .isRead(isRead)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private NotificationResponse buildResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .userId(n.getUserId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .isRead(n.getIsRead())
                .build();
    }

    @Nested
    @DisplayName("sendNotification()")
    class SendNotificationTests {

        @Test
        @DisplayName("should save and return notification")
        void shouldSaveAndReturnNotification() {
            Notification saved = buildNotification(1L, false);
            NotificationResponse response = buildResponse(saved);

            when(notificationRepository.save(any(Notification.class))).thenReturn(saved);
            when(notificationMapper.toResponse(saved)).thenReturn(response);

            NotificationResponse result = notificationService.sendNotification(
                    1L, "user@example.com", "APPLICATION_SUBMITTED",
                    "Application Submitted", "Your application has been submitted.",
                    100L, "APPLICATION", "/candidate/applications/100"
            );

            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo("APPLICATION_SUBMITTED");
            assertThat(result.getIsRead()).isFalse();
            verify(notificationRepository).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("markAsRead()")
    class MarkAsReadTests {

        @Test
        @DisplayName("should mark notification as read")
        void shouldMarkAsRead() {
            Notification notification = buildNotification(1L, true);
            NotificationResponse response = buildResponse(notification);

            when(notificationRepository.markAsRead(1L, 1L)).thenReturn(1);
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
            when(notificationMapper.toResponse(notification)).thenReturn(response);

            NotificationResponse result = notificationService.markAsRead(1L, 1L);

            assertThat(result.getIsRead()).isTrue();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when notification not found")
        void shouldThrowWhenNotificationNotFound() {
            when(notificationRepository.markAsRead(999L, 1L)).thenReturn(0);

            assertThatThrownBy(() -> notificationService.markAsRead(999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("markAllAsRead()")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("should mark all notifications as read and return count")
        void shouldMarkAllAsRead() {
            when(notificationRepository.markAllAsRead(1L)).thenReturn(5);

            int count = notificationService.markAllAsRead(1L);

            assertThat(count).isEqualTo(5);
            verify(notificationRepository).markAllAsRead(1L);
        }
    }

    @Nested
    @DisplayName("getUnreadCount()")
    class UnreadCountTests {

        @Test
        @DisplayName("should return correct unread count")
        void shouldReturnCorrectUnreadCount() {
            when(notificationRepository.countByUserIdAndIsRead(1L, false)).thenReturn(7L);

            UnreadCountResponse result = notificationService.getUnreadCount(1L);

            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getUnreadCount()).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("getUnreadNotificationsByUser()")
    class UnreadNotificationsTests {

        @Test
        @DisplayName("should return list of unread notifications")
        void shouldReturnUnreadNotifications() {
            Notification n = buildNotification(1L, false);
            NotificationResponse response = buildResponse(n);

            when(notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(1L, false))
                    .thenReturn(List.of(n));
            when(notificationMapper.toResponse(n)).thenReturn(response);

            List<NotificationResponse> result = notificationService.getUnreadNotificationsByUser(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIsRead()).isFalse();
        }
    }

    @Test
    @DisplayName("deleteNotification() should throw when notification not found")
    void shouldThrowWhenDeletingNonExistentNotification() {
        when(notificationRepository.deleteByNotificationIdAndUserId(999L, 1L)).thenReturn(0);

        assertThatThrownBy(() -> notificationService.deleteNotification(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
