package com.aerionsoft.notification.enums;

import com.aerionsoft.notification.entity.NotificationType;

public enum TourNotificationType implements NotificationType {
    APPLICATION_UPDATE;

    @Override public String getCode() { return "TOUR_" + name(); }
    @Override public NotificationCategory getCategory() { return NotificationCategory.TOUR; }
}
