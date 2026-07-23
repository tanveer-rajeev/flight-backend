package com.aerionsoft.notification.enums;

import com.aerionsoft.notification.entity.NotificationType;

public enum CreditNotificationType implements NotificationType {
    REQUESTED,
    APPROVED,
    REJECTED;

    @Override public String getCode() { return "CREDIT_" + name(); }
    @Override public NotificationCategory getCategory() { return NotificationCategory.CREDIT; }
}
