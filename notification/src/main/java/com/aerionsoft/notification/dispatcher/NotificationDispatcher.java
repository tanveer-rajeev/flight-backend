package com.aerionsoft.notification.dispatcher;

import com.aerionsoft.notification.entity.Notification;
import com.aerionsoft.notification.enums.NotificationChannelType;

import java.util.Map;

public interface NotificationDispatcher {

    void dispatch(Notification notification, Map<NotificationChannelType, String> recipientContacts);
}