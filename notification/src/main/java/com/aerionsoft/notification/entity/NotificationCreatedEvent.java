package com.aerionsoft.notification.entity;

import java.util.Objects;

public record NotificationCreatedEvent(
        Long notificationId,
        Long recipientUserId,
        NotificationType type
) {
    public NotificationCreatedEvent {
        Objects.requireNonNull(notificationId, "notificationId must not be null");
        Objects.requireNonNull(recipientUserId, "userId must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }

    public static NotificationCreatedEvent from(Long notificationId, Long recipientUserId, NotificationType type) {
        return new NotificationCreatedEvent(notificationId, recipientUserId, type);
    }
}
