package com.aerionsoft.notification.dto;

public enum CreditNotificationType implements NotificationType {
    REQUESTED,
    APPROVED,
    REJECTED;

    @Override public String getCode() { return "CREDIT_" + name(); }
    @Override public NotificationCategory getCategory() { return NotificationCategory.CREDIT; }
}
