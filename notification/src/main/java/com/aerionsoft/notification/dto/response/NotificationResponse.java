package com.aerionsoft.notification.dto.response;

import com.aerionsoft.notification.enums.NotificationPriority;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String typeCode,
        String title,
        String message,
        NotificationPriority priority,
        boolean read,
        String referenceType,
        String referenceId,
        String actionUrl,
        String actionLabel,
        LocalDateTime createdAt
) {
}