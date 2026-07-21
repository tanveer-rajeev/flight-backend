package com.aerionsoft.application.dto.notification;

import com.aerionsoft.application.enums.notification.NotificationPriority;
import com.aerionsoft.application.enums.notification.NotificationStatus;
import com.aerionsoft.application.enums.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private Long id;
    private Long userId;
    private Long businessId;
    private NotificationType type;
    private NotificationPriority priority;
    private NotificationStatus status;
    private String title;
    private String message;
    private String actionUrl;
    private String actionLabel;
    private String referenceId;
    private String referenceType;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private LocalDateTime archivedAt;
    private LocalDateTime expiresAt;
    private Long createdBy;
}

