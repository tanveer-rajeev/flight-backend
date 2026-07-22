package com.aerionsoft.notification.dto;

import java.time.LocalDateTime;

public record NotificationSocketMessage(
        Long notificationId,
        NotificationType type,
        String title,
        String body,
        LocalDateTime createdAt
) {
}
