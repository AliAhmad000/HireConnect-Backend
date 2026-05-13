package com.hireconnect.notification.controller;

import com.hireconnect.notification.dto.request.SendNotificationRequest;
import org.springframework.security.core.Authentication;
import com.hireconnect.notification.dto.response.ApiResponse;
import com.hireconnect.notification.dto.response.NotificationResponse;
import com.hireconnect.notification.dto.response.UnreadCountResponse;
import com.hireconnect.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/unread/count")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getMyUnreadCount(
            Authentication authentication) {

        Long userId =
                Long.parseLong(authentication.getName());

        UnreadCountResponse count =
                notificationService.getUnreadCount(userId);

        return ResponseEntity.ok(ApiResponse.success(count));
    }
    /** Send a notification manually (Admin / internal use) */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {
        NotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Notification sent", response));
    }

    /** Get all notifications for a user (paginated) */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = Long.parseLong(authentication.getName());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<NotificationResponse> result = notificationService.getNotificationsByUser(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** Get all notifications for a user (paginated) */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<NotificationResponse> result = notificationService.getNotificationsByUser(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** Get unread notifications for a user */
    @GetMapping("/user/{userId}/unread")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotifications(
            @PathVariable Long userId) {
        List<NotificationResponse> result = notificationService.getUnreadNotificationsByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** Get unread count badge */
    @GetMapping("/user/{userId}/unread/count")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(@PathVariable Long userId) {
        UnreadCountResponse count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /** Mark a single notification as read */
    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long notificationId,
            @RequestHeader("X-User-Id") Long userId) {
        NotificationResponse response = notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", response));
    }

    /** Mark all notifications as read */
    @PatchMapping("/read-all")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> markMyNotificationsAsRead(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", count));
    }

    /** Mark all notifications as read */
    @PatchMapping("/user/{userId}/read-all")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(@PathVariable Long userId) {
        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", count));
    }

    /** Delete a notification */
    @DeleteMapping("/{notificationId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable Long notificationId,
            @RequestHeader("X-User-Id") Long userId) {
        notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }
}
