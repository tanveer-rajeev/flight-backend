package com.aerionsoft.notification.enums;

import com.aerionsoft.notification.entity.NotificationType;

public enum VisaNotificationType implements NotificationType {
    APPLICATION_UPDATE;

    @Override public String getCode() { return "VISA_" + name(); }
    @Override public NotificationCategory getCategory() { return NotificationCategory.VISA; }
}
