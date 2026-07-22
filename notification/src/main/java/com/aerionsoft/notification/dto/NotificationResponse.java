package com.aerionsoft.notification.dto;

import com.aerionsoft.notification.enums.NotificationPriority;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String body,
        NotificationPriority priority,
        boolean read,
        LocalDateTime createdAt
) {
}
