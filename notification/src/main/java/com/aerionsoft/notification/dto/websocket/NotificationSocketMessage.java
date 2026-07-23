package com.aerionsoft.notification.dto.websocket;

import java.time.LocalDateTime;

public record NotificationSocketMessage(
        Long notificationId,
        String typeCode,
        String title,
        String message,
        String referenceType,
        String referenceId,
        String actionUrl,
        String actionLabel,
        LocalDateTime createdAt
) {
}