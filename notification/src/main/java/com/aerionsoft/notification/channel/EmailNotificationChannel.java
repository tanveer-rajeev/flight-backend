package com.aerionsoft.notification.channel;

import com.aerionsoft.notification.email.EmailNotificationSender;
import com.aerionsoft.notification.entity.Notification;
import com.aerionsoft.notification.entity.NotificationDelivery;
import com.aerionsoft.notification.enums.NotificationChannelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationChannel implements NotificationChannel {

    private final EmailNotificationSender emailSender;

    public EmailNotificationChannel(EmailNotificationSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public NotificationChannelType getType() {
        return NotificationChannelType.EMAIL;
    }

    @Override
    public void send(Notification notification, NotificationDelivery delivery) {
        try {
            emailSender.send(delivery.getRecipient(), notification.getTitle(), notification.getMessage());
            delivery.markSent();
        } catch (Exception e) {
            log.error("Failed to send email notification id={} to recipient={}", notification.getId(), delivery.getRecipient(), e);
            delivery.markFailed(e.getMessage());
        }
    }
}
