package com.aerionsoft.notification.dispatcher.recipient;

import com.aerionsoft.notification.entity.Notification;
import com.aerionsoft.notification.enums.NotificationChannelType;

import java.util.Map;

public interface NotificationRecipientResolver {

    NotificationChannelType getChannelType();

    String resolveRecipient(Notification notification, Map<NotificationChannelType, String> recipientContacts);
}