package com.aerionsoft.notification.enums;

import com.aerionsoft.notification.entity.NotificationType;

public enum AccountNotificationType implements NotificationType {
    UPDATED,
    VERIFIED,
    WELCOME_USER,
    LOGIN_ALERT;

    @Override public String getCode() { return "ACCOUNT_" + name(); }
    @Override public NotificationCategory getCategory() { return NotificationCategory.ACCOUNT; }
}
