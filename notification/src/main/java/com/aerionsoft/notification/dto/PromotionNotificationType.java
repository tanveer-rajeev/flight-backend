package com.aerionsoft.notification.dto;

public enum PromotionNotificationType implements NotificationType {
    OFFER;

    @Override public String getCode() { return "PROMOTION_" + name(); }
    @Override public NotificationCategory getCategory() { return NotificationCategory.PROMOTION; }
}
