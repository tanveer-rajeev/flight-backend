package com.aerionsoft.application.controller.notification;

import com.aerionsoft.application.controller.BaseController;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.notification.*;
import com.aerionsoft.application.enums.notification.NotificationStatus;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.service.notification.NotificationPreferenceService;
import com.aerionsoft.application.service.notification.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController extends BaseController {

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;

    /**
     * Create a new notification
     */
    @PostMapping
    public ResponseEntity<BaseResponse<NotificationDTO>> createNotification(
            @Valid @RequestBody CreateNotificationRequest request) {
        NotificationDTO notification = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Notification created successfully", notification));
    }

    /**
     * Create notification from template
     */
    @PostMapping("/from-template")
    public ResponseEntity<BaseResponse<NotificationDTO>> createFromTemplate(
            @Valid @RequestBody CreateNotificationFromTemplateRequest request) {
        NotificationDTO notification = notificationService.createFromTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Notification created from template successfully", notification));
    }

    /**
     * Create notification from template with email option
     */
    @PostMapping("/from-template/with-email")
    public ResponseEntity<BaseResponse<NotificationDTO>> createFromTemplateWithEmail(
            @Valid @RequestBody CreateNotificationFromTemplateRequest request,
            @RequestParam String userEmail,
            @RequestParam(defaultValue = "true") boolean sendEmail) {
        NotificationDTO notification = notificationService.createFromTemplate(request, userEmail, sendEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Notification created from template successfully", notification));
    }

    /**
     * Get user notifications with pagination
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<BaseResponse<Page<NotificationDTO>>> getUserNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<NotificationDTO> notifications = notificationService.getUserNotifications(userId, page, size);
        return ResponseEntity.ok(BaseResponse.ok(notifications));
    }

    /**
     * Get notifications for the currently logged-in user (no userId needed)
     */
    @GetMapping("/me")
    public ResponseEntity<BaseResponse<Page<NotificationDTO>>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserIdFromAuthentication();
        Page<NotificationDTO> notifications = notificationService.getUserNotifications(userId, page, size);
        return ResponseEntity.ok(BaseResponse.ok(notifications));
    }

    /**
     * Get unread count for the currently logged-in user
     */
    @GetMapping("/me/unread-count")
    public ResponseEntity<BaseResponse<Map<String, Long>>> getMyUnreadCount() {
        Long userId = getUserIdFromAuthentication();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(BaseResponse.ok(Map.of("unreadCount", count)));
    }

    /**
     * Get recent unread notifications for the currently logged-in user
     */
    @GetMapping("/me/recent-unread")
    public ResponseEntity<BaseResponse<List<NotificationDTO>>> getMyRecentUnread(
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = getUserIdFromAuthentication();
        List<NotificationDTO> notifications = notificationService.getRecentUnreadNotifications(userId, limit);
        return ResponseEntity.ok(BaseResponse.ok(notifications));
    }

    /**
     * Get summary for the currently logged-in user
     */
    @GetMapping("/me/summary")
    public ResponseEntity<BaseResponse<NotificationSummaryDTO>> getMySummary() {
        Long userId = getUserIdFromAuthentication();
        NotificationSummaryDTO summary = notificationService.getNotificationSummary(userId);
        return ResponseEntity.ok(BaseResponse.ok(summary));
    }

    /**
     * Mark all as read for the currently logged-in user
     */
    @PutMapping("/me/read-all")
    public ResponseEntity<BaseResponse<Map<String, Object>>> markMyAllAsRead() {
        Long userId = getUserIdFromAuthentication();
        int updated = notificationService.markAllAsRead(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "All notifications marked as read");
        data.put("updatedCount", updated);
        return ResponseEntity.ok(BaseResponse.ok(data));
    }

    /**
     * Get unread notifications
     */
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<BaseResponse<Page<NotificationDTO>>> getUnreadNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<NotificationDTO> notifications = notificationService.getNotificationsByStatus(
                userId, NotificationStatus.UNREAD, page, size);
        return ResponseEntity.ok(BaseResponse.ok(notifications));
    }

    /**
     * Get recent unread notifications (limited)
     */
    @GetMapping("/user/{userId}/recent-unread")
    public ResponseEntity<BaseResponse<List<NotificationDTO>>> getRecentUnreadNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        List<NotificationDTO> notifications = notificationService.getRecentUnreadNotifications(userId, limit);
        return ResponseEntity.ok(BaseResponse.ok(notifications));
    }

    /**
     * Get filtered notifications
     */
    @PostMapping("/user/{userId}/filter")
    public ResponseEntity<BaseResponse<Page<NotificationDTO>>> getFilteredNotifications(
            @PathVariable Long userId,
            @Valid @RequestBody NotificationFilterRequest filter) {
        Page<NotificationDTO> notifications = notificationService.getFilteredNotifications(userId, filter);
        return ResponseEntity.ok(BaseResponse.ok(notifications));
    }

    /**
     * Get notification by ID
     */
    @GetMapping("/{id}/user/{userId}")
    public ResponseEntity<BaseResponse<NotificationDTO>> getNotificationById(
            @PathVariable Long id,
            @PathVariable Long userId) {
        NotificationDTO notification = notificationService.getNotificationById(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        return ResponseEntity.ok(BaseResponse.ok(notification));
    }

    /**
     * Get unread count
     */
    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<BaseResponse<Map<String, Long>>> getUnreadCount(@PathVariable Long userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(BaseResponse.ok(Map.of("unreadCount", count)));
    }

    /**
     * Get notification summary
     */
    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<BaseResponse<NotificationSummaryDTO>> getNotificationSummary(@PathVariable Long userId) {
        NotificationSummaryDTO summary = notificationService.getNotificationSummary(userId);
        return ResponseEntity.ok(BaseResponse.ok(summary));
    }

    /**
     * Mark notification as read
     */
    @PutMapping("/{id}/user/{userId}/read")
    public ResponseEntity<BaseResponse<Map<String, String>>> markAsRead(
            @PathVariable Long id,
            @PathVariable Long userId) {
        if (!notificationService.markAsRead(id, userId)) {
            throw new ResourceNotFoundException("Notification", id);
        }
        return ResponseEntity.ok(BaseResponse.ok(Map.of("message", "Notification marked as read")));
    }

    /**
     * Mark all notifications as read
     */
    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<BaseResponse<Map<String, Object>>> markAllAsRead(@PathVariable Long userId) {
        int updated = notificationService.markAllAsRead(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "All notifications marked as read");
        data.put("updatedCount", updated);
        return ResponseEntity.ok(BaseResponse.ok(data));
    }

    /**
     * Mark notification as archived
     */
    @PutMapping("/{id}/user/{userId}/archive")
    public ResponseEntity<BaseResponse<Map<String, String>>> markAsArchived(
            @PathVariable Long id,
            @PathVariable Long userId) {
        if (!notificationService.markAsArchived(id, userId)) {
            throw new ResourceNotFoundException("Notification", id);
        }
        return ResponseEntity.ok(BaseResponse.ok(Map.of("message", "Notification archived")));
    }

    /**
     * Delete notification
     */
    @DeleteMapping("/{id}/user/{userId}")
    public ResponseEntity<BaseResponse<Map<String, String>>> deleteNotification(
            @PathVariable Long id,
            @PathVariable Long userId) {
        if (!notificationService.deleteNotification(id, userId)) {
            throw new ResourceNotFoundException("Notification", id);
        }
        return ResponseEntity.ok(BaseResponse.ok(Map.of("message", "Notification deleted")));
    }

    /**
     * Get user notification preferences
     */
    @GetMapping("/preferences/{userId}")
    public ResponseEntity<BaseResponse<NotificationPreferenceDTO>> getPreferences(@PathVariable Long userId) {
        NotificationPreferenceDTO preferences = preferenceService.getOrCreatePreferences(userId);
        return ResponseEntity.ok(BaseResponse.ok(preferences));
    }

    /**
     * Update user notification preferences
     */
    @PutMapping("/preferences/{userId}")
    public ResponseEntity<BaseResponse<NotificationPreferenceDTO>> updatePreferences(
            @PathVariable Long userId,
            @Valid @RequestBody NotificationPreferenceDTO preferenceDTO) {
        NotificationPreferenceDTO updated = preferenceService.updatePreferences(userId, preferenceDTO);
        return ResponseEntity.ok(BaseResponse.ok(updated));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<BaseResponse<Map<String, String>>> health() {
        Map<String, String> data = Map.of(
                "status", "UP",
                "service", "Notification Service"
        );
        return ResponseEntity.ok(BaseResponse.ok(data));
    }
}
