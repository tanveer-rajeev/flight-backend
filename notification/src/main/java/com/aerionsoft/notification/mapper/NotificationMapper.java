package com.aerionsoft.notification.mapper;

import com.aerionsoft.notification.dto.NotificationResponse;
import com.aerionsoft.notification.dto.NotificationSocketMessage;
import com.aerionsoft.notification.dto.NotificationSummaryResponse;
import com.aerionsoft.notification.entity.Notification;

import java.util.List;

public class NotificationMapper {

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getPriority(),
                notification.isReadFlag(),
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
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getCreatedAt()
        );
    }
}
