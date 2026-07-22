package com.aerionsoft.notification.dto;

public enum VisaNotificationType implements NotificationType {
    APPLICATION_UPDATE;

    @Override public String getCode() { return "VISA_" + name(); }
    @Override public NotificationCategory getCategory() { return NotificationCategory.VISA; }
}
