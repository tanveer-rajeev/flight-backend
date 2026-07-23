package com.aerionsoft.notification.channel;

import com.aerionsoft.notification.entity.Notification;
import com.aerionsoft.notification.entity.NotificationDelivery;
import com.aerionsoft.notification.enums.NotificationChannelType;

public interface NotificationChannel {

    NotificationChannelType getType();

    void send(Notification notification, NotificationDelivery delivery);

    default boolean isDefaultEnabled() {
        return true;
    }
}
