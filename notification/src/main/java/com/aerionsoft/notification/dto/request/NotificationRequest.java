package com.aerionsoft.notification.dto.request;

import com.aerionsoft.notification.entity.NotificationType;
import com.aerionsoft.notification.enums.NotificationChannelType;
import com.aerionsoft.notification.enums.NotificationPriority;

import java.util.Map;
import java.util.Objects;

public record NotificationRequest(
        Long userId,
        NotificationType type,
        String title,
        String message,
        NotificationPriority priority,
        Map<String, Object> metadata,
        Map<NotificationChannelType, String> recipientContacts,
        String referenceType,
        String referenceId,
        String actionUrl,
        String actionLabel
) {
    public NotificationRequest {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        metadata = (metadata != null) ? Map.copyOf(metadata) : Map.of();
        recipientContacts = (recipientContacts != null) ? Map.copyOf(recipientContacts) : Map.of();
    }
}