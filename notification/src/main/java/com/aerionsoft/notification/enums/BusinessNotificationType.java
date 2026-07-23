package com.aerionsoft.notification.enums;

import com.aerionsoft.notification.entity.NotificationType;

public enum BusinessNotificationType implements NotificationType {
    CREATED,
    APPROVED,
    REJECTED;

    @Override public String getCode() { return "BUSINESS_" + name(); }
    @Override public NotificationCategory getCategory() { return NotificationCategory.BUSINESS; }
}
