package com.aerionsoft.notification.dto;

public enum AgencyNotificationType implements NotificationType {
    APPROVED,
    REJECTED;

    @Override public String getCode() { return "AGENCY_" + name(); }
    @Override public NotificationCategory getCategory() { return NotificationCategory.AGENCY; }
}
