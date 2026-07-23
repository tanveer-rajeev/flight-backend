package com.aerionsoft.notification.enums;

import com.aerionsoft.notification.entity.NotificationType;

public enum SystemNotificationType implements NotificationType {
    ALERT,
    GENERAL;

    @Override public String getCode() { return "SYSTEM_" + name(); }
    @Override public NotificationCategory getCategory() { return NotificationCategory.SYSTEM; }
}
