package com.aerionsoft.notification.dto;

public enum PaymentNotificationType implements NotificationType {
    SUCCESS,
    FAILED;

    @Override public String getCode() { return "PAYMENT_" + name(); }
    @Override public NotificationCategory getCategory() { return NotificationCategory.PAYMENT; }
}
