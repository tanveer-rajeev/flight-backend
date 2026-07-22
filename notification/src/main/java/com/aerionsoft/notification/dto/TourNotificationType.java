package com.aerionsoft.notification.dto;

public enum TourNotificationType implements NotificationType {
    APPLICATION_UPDATE;

    @Override public String getCode() { return "TOUR_" + name(); }
    @Override public NotificationCategory getCategory() { return NotificationCategory.TOUR; }
}
