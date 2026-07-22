package com.aerionsoft.notification.dto;

import com.aerionsoft.notification.enums.NotificationPriority;

import java.util.Map;
import java.util.Objects;

public record NotificationRequest(
        Long recipientUserId,
        NotificationType type,
        String title,
        String message,
        NotificationPriority priority,
        Map<String, Object> metadata
) {
    public NotificationRequest {
        Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(message, "body must not be null");
        priority = (priority != null) ? priority : NotificationPriority.NORMAL;
        metadata = (metadata != null) ? Map.copyOf(metadata) : Map.of();
    }
}
