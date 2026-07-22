package com.aerionsoft.notification.websocket;

import com.aerionsoft.notification.entity.Notification;

public interface NotificationWebSocketPublisher {
    void publish(Notification notification);
}
