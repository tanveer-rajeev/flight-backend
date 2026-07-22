package com.aerionsoft.notification.dto;

public enum BusinessNotificationType implements NotificationType {
    CREATED,
    APPROVED,
    REJECTED;

    @Override public String getCode() { return "BUSINESS_" + name(); }
    @Override public NotificationCategory getCategory() { return NotificationCategory.BUSINESS; }
}
