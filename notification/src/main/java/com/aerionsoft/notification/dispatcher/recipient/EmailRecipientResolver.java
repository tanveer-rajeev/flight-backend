package com.aerionsoft.notification.dispatcher.recipient;

import com.aerionsoft.notification.entity.Notification;
import com.aerionsoft.notification.enums.NotificationChannelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class EmailRecipientResolver implements NotificationRecipientResolver {

    @Override
    public NotificationChannelType getChannelType() {
        return NotificationChannelType.EMAIL;
    }

    @Override
    public String resolveRecipient(Notification notification, Map<NotificationChannelType, String> recipientContacts) {
        String email = recipientContacts.get(NotificationChannelType.EMAIL);
        if (email == null || email.isBlank()) {
            log.warn("No email address supplied for notification id={}, userId={} — EMAIL channel will fail",
                    notification.getId(), notification.getUserId());
        }
        return email;
    }
}