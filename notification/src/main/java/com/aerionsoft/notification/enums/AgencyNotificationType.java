package com.aerionsoft.notification.enums;

import com.aerionsoft.notification.entity.NotificationType;

public enum AgencyNotificationType implements NotificationType {
    APPROVED,
    REJECTED;

    @Override public String getCode() { return "AGENCY_" + name(); }
    @Override public NotificationCategory getCategory() { return NotificationCategory.AGENCY; }
}
