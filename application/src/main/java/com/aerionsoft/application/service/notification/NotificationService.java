package com.aerionsoft.application.service.notification;

import com.aerionsoft.application.dto.notification.*;
import com.aerionsoft.application.util.TimestampMapper;
import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.entity.Notification;
import com.aerionsoft.application.entity.NotificationTemplate;
import com.aerionsoft.application.enums.notification.NotificationPriority;
import com.aerionsoft.application.enums.notification.NotificationStatus;
import com.aerionsoft.application.repository.notification.NotificationPreferenceRepository;
import com.aerionsoft.application.repository.notification.NotificationRepository;
import com.aerionsoft.application.repository.notification.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationEmailService emailService;
    private final TimestampMapper timestampMapper;

    /**
     * Create a new notification
     */
    @Transactional
    public NotificationDTO createNotification(CreateNotificationRequest request) {
        log.info("Creating notification for user: {}, type: {}", request.getUserId(), request.getType());

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .businessId(request.getBusinessId())
                .type(request.getType())
                .priority(request.getPriority() != null ? request.getPriority() : NotificationPriority.MEDIUM)
                .status(NotificationStatus.UNREAD)
                .title(request.getTitle())
                .message(request.getMessage())
                .actionUrl(request.getActionUrl())
                .actionLabel(request.getActionLabel())
                .referenceId(request.getReferenceId())
                .referenceType(request.getReferenceType())
                .metadata(request.getMetadata())
                .expiresAt(request.getExpiresAt())
                .createdBy(request.getCreatedBy())
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification created successfully with ID: {}", notification.getId());

        return mapToDTO(notification);
    }

    /**
     * Create a new notification with email sending option
     */
    @Transactional
    public NotificationDTO createNotification(CreateNotificationRequest request, String userEmail, boolean sendEmail) {
        NotificationDTO notificationDTO = createNotification(request);

        if (sendEmail && userEmail != null && !userEmail.isEmpty()) {
            emailService.sendEmailNotification(
                notificationDTO.getId(),
                request.getUserId(),
                userEmail,
                request.getTitle(),
                request.getMessage(),
                request.getActionUrl()
            );
        }

        return notificationDTO;
    }

    /**
     * Create notification from template
     */
    public NotificationDTO createFromTemplate(CreateNotificationFromTemplateRequest request) {
        log.info("Creating notification from template: {}", request.getTemplateCode());

        NotificationTemplate template = templateRepository.findByTemplateCodeAndIsActive(request.getTemplateCode(), true)
                .orElseThrow(() -> new ResourceNotFoundException("Template", request.getTemplateCode()));

        String title = replaceVariables(template.getTitleTemplate(), request.getVariables());
        String message = replaceVariables(template.getMessageTemplate(), request.getVariables());
        String actionUrl = template.getActionUrlTemplate() != null
                ? replaceVariables(template.getActionUrlTemplate(), request.getVariables())
                : null;

        CreateNotificationRequest notificationRequest = CreateNotificationRequest.builder()
                .userId(request.getUserId())
                .businessId(request.getBusinessId())
                .type(template.getType())
                .priority(template.getPriority())
                .title(title)
                .message(message)
                .actionUrl(actionUrl)
                .actionLabel(template.getActionLabel())
                .createdBy(request.getCreatedBy())
                .build();

        return createNotification(notificationRequest);
    }

    /**
     * Create notification from template with email sending option
     */
    public NotificationDTO createFromTemplate(CreateNotificationFromTemplateRequest request, String userEmail, boolean sendEmail) {
        log.info("Creating notification from template: {} with email option: {}", request.getTemplateCode(), sendEmail);

        NotificationTemplate template = templateRepository.findByTemplateCodeAndIsActive(request.getTemplateCode(), true)
                .orElseThrow(() -> new ResourceNotFoundException("Template", request.getTemplateCode()));

        String title = replaceVariables(template.getTitleTemplate(), request.getVariables());
        String message = replaceVariables(template.getMessageTemplate(), request.getVariables());
        String actionUrl = template.getActionUrlTemplate() != null
                ? replaceVariables(template.getActionUrlTemplate(), request.getVariables())
                : null;

        CreateNotificationRequest notificationRequest = CreateNotificationRequest.builder()
                .userId(request.getUserId())
                .businessId(request.getBusinessId())
                .type(template.getType())
                .priority(template.getPriority())
                .title(title)
                .message(message)
                .actionUrl(actionUrl)
                .actionLabel(template.getActionLabel())
                .createdBy(request.getCreatedBy())
                .build();

        return createNotification(notificationRequest, userEmail, sendEmail);
    }

    /**
     * Get user notifications with pagination (unread only)
     */
    public Page<NotificationDTO> getUserNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.UNREAD, pageable);
        return notifications.map(this::mapToDTO);
    }

    /**
     * Get notifications by status
     */
    public Page<NotificationDTO> getNotificationsByStatus(Long userId, NotificationStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        return notifications.map(this::mapToDTO);
    }

    /**
     * Get filtered notifications
     */
    public Page<NotificationDTO> getFilteredNotifications(Long userId, NotificationFilterRequest filter) {
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize());
        Page<Notification> notifications = notificationRepository.findByFilters(
                userId,
                filter.getType(),
                filter.getStatus(),
                filter.getPriority(),
                filter.getFromDate(),
                filter.getToDate(),
                filter.getReferenceType(),
                pageable
        );
        return notifications.map(this::mapToDTO);
    }

    /**
     * Get notification by ID
     */
    public Optional<NotificationDTO> getNotificationById(Long id, Long userId) {
        return notificationRepository.findByIdAndUserId(id, userId)
                .map(this::mapToDTO);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public boolean markAsRead(Long notificationId, Long userId) {
        log.info("Marking notification {} as read for user {}", notificationId, userId);
        int updated = notificationRepository.markAsRead(notificationId, userId, UserDateTimeUtil.now());
        return updated > 0;
    }

    /**
     * Mark all notifications as read
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        log.info("Marking all notifications as read for user {}", userId);
        return notificationRepository.markAllAsRead(userId, UserDateTimeUtil.now());
    }

    /**
     * Mark notification as archived
     */
    @Transactional
    public boolean markAsArchived(Long notificationId, Long userId) {
        log.info("Archiving notification {} for user {}", notificationId, userId);
        int updated = notificationRepository.markAsArchived(notificationId, userId, UserDateTimeUtil.now());
        return updated > 0;
    }

    /**
     * Delete notification
     */
    @Transactional
    public boolean deleteNotification(Long notificationId, Long userId) {
        log.info("Deleting notification {} for user {}", notificationId, userId);
        Optional<Notification> notification = notificationRepository.findByIdAndUserId(notificationId, userId);
        if (notification.isPresent()) {
            notificationRepository.delete(notification.get());
            return true;
        }
        return false;
    }

    /**
     * Get unread count
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    /**
     * Get notification summary
     */
    public NotificationSummaryDTO getNotificationSummary(Long userId) {
        Object summary = notificationRepository.getSummaryByUserId(userId);

        if (summary instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Long> summaryMap = (Map<String, Long>) summary;

            return NotificationSummaryDTO.builder()
                    .totalNotifications(summaryMap.getOrDefault("total", 0L))
                    .unreadCount(summaryMap.getOrDefault("unread", 0L))
                    .readCount(summaryMap.getOrDefault("read", 0L))
                    .archivedCount(summaryMap.getOrDefault("archived", 0L))
                    .urgentCount(summaryMap.getOrDefault("urgent", 0L))
                    .highPriorityCount(summaryMap.getOrDefault("high", 0L))
                    .build();
        }

        return NotificationSummaryDTO.builder()
                .totalNotifications(0L)
                .unreadCount(0L)
                .readCount(0L)
                .archivedCount(0L)
                .urgentCount(0L)
                .highPriorityCount(0L)
                .build();
    }

    /**
     * Get recent unread notifications
     */
    public List<NotificationDTO> getRecentUnreadNotifications(Long userId, int limit) {
        List<Notification> notifications = notificationRepository
                .findTop10ByUserIdAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.UNREAD);
        return notifications.stream()
                .limit(limit)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Cleanup expired and old notifications
     */
    @Transactional
    public void cleanupNotifications() {
        log.info("Starting notification cleanup");

        // Delete expired notifications
        int expiredDeleted = notificationRepository.deleteExpiredNotifications(UserDateTimeUtil.now());
        log.info("Deleted {} expired notifications", expiredDeleted);

        // Delete old archived notifications (90 days)
        LocalDateTime cutoffDate = UserDateTimeUtil.now().minusDays(90);
        int archivedDeleted = notificationRepository.deleteOldArchivedNotifications(cutoffDate);
        log.info("Deleted {} old archived notifications", archivedDeleted);
    }

    /**
     * Replace template variables
     */
    private String replaceVariables(String template, Map<String, String> variables) {
        if (template == null) {
            return null;
        }
        if (variables == null || variables.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    /**
     * Map entity to DTO
     */
    private NotificationDTO mapToDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .businessId(notification.getBusinessId())
                .type(notification.getType())
                .priority(notification.getPriority())
                .status(notification.getStatus())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .actionUrl(notification.getActionUrl())
                .actionLabel(notification.getActionLabel())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .metadata(notification.getMetadata())
                .createdAt(timestampMapper.createdAt(notification))
                .readAt(timestampMapper.toRequestUserTime(notification.getReadAt(), notification.getCreatedTimeOffset()))
                .archivedAt(timestampMapper.toRequestUserTime(notification.getArchivedAt(), notification.getCreatedTimeOffset()))
                .expiresAt(timestampMapper.toRequestUserTime(notification.getExpiresAt(), notification.getCreatedTimeOffset()))
                .createdBy(notification.getCreatedBy())
                .build();
    }
}

