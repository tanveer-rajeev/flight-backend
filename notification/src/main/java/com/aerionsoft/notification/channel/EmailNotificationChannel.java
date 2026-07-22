package com.aerionsoft.notification.channel;

import com.aerionsoft.notification.entity.Notification;
import com.aerionsoft.notification.entity.NotificationDelivery;
import com.aerionsoft.notification.enums.NotificationChannelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationChannel implements NotificationChannel{
    @Override
    public NotificationChannelType getType() {
        return null;
    }

    @Override
    public void send(Notification notification, NotificationDelivery delivery) {

    }
}
