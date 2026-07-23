package com.aerionsoft.notification.enums;

import com.aerionsoft.notification.entity.NotificationType;

public enum PaymentNotificationType implements NotificationType {
    SUCCESS,
    FAILED;

    @Override public String getCode() { return "PAYMENT_" + name(); }
    @Override public NotificationCategory getCategory() { return NotificationCategory.PAYMENT; }
}
