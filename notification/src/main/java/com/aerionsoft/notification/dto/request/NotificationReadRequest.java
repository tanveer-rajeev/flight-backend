package com.aerionsoft.notification.dto.request;

import com.aerionsoft.notification.exception.NotificationNotFoundException;

import java.util.List;

public record NotificationReadRequest(List<Long> notificationIds) {
    public NotificationReadRequest {
        if (notificationIds == null || notificationIds.isEmpty()) {
            throw new NotificationNotFoundException("Notification id's not found");
        }
    }
}
