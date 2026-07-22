package com.aerionsoft.notification.channel;

import com.aerionsoft.notification.entity.Notification;
import com.aerionsoft.notification.entity.NotificationDelivery;
import com.aerionsoft.notification.enums.NotificationChannelType;
import com.aerionsoft.notification.websocket.NotificationWebSocketPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InAppNotificationChannel implements NotificationChannel {
    private final NotificationWebSocketPublisher webSocketPublisher;

    @Override
    public NotificationChannelType getType() {
        return NotificationChannelType.IN_APP;
    }

    @Override
    public void send(Notification notification, NotificationDelivery delivery) {
        try {
            webSocketPublisher.publish(notification);
            delivery.markSent();
        } catch (Exception e) {
            log.error("Failed to push in-app notification id={} to userId={}", notification.getId(), notification.getUserId(), e);
            delivery.markFailed(e.getMessage());
        }
    }
}
