package com.aerionsoft.notification.mapper;

import com.aerionsoft.notification.dto.response.NotificationResponse;
import com.aerionsoft.notification.dto.response.NotificationSummaryResponse;
import com.aerionsoft.notification.dto.websocket.NotificationSocketMessage;
import com.aerionsoft.notification.entity.Notification;

import java.util.List;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTypeCode(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getPriority(),
                notification.isReadFlag(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getActionUrl(),
                notification.getActionLabel(),
                notification.getCreatedAt()
        );
    }

    public static List<NotificationResponse> toResponseList(List<Notification> notifications) {
        return notifications.stream()
                .map(NotificationMapper::toResponse)
                .toList();
    }

    public static NotificationSummaryResponse toSummaryResponse(long unreadCount, List<Notification> recent) {
        return new NotificationSummaryResponse(unreadCount, toResponseList(recent));
    }

    public static NotificationSocketMessage toSocketMessage(Notification notification) {
        return new NotificationSocketMessage(
                notification.getId(),
                notification.getTypeCode(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getActionUrl(),
                notification.getActionLabel(),
                notification.getCreatedAt()
        );
    }
}