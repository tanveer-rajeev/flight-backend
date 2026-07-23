package com.aerionsoft.notification.dispatcher.recipient;

import com.aerionsoft.notification.entity.Notification;
import com.aerionsoft.notification.enums.NotificationChannelType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class InAppRecipientResolver implements NotificationRecipientResolver {

    @Override
    public NotificationChannelType getChannelType() {
        return NotificationChannelType.IN_APP;
    }

    @Override
    public String resolveRecipient(Notification notification, Map<NotificationChannelType, String> recipientContacts) {
        return notification.getUserId().toString();
    }
}