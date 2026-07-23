package com.aerionsoft.notification.enums;

import com.aerionsoft.notification.entity.NotificationType;

public enum PromotionNotificationType implements NotificationType {
    OFFER;

    @Override public String getCode() { return "PROMOTION_" + name(); }
    @Override public NotificationCategory getCategory() { return NotificationCategory.PROMOTION; }
}
