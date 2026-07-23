package com.aerionsoft.notification.entity;

import com.aerionsoft.notification.enums.NotificationCategory;

public interface NotificationType {
    String name();
    String getCode();
    NotificationCategory getCategory();
}
