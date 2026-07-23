package com.aerionsoft.notification.enums;

import com.aerionsoft.notification.entity.NotificationType;

public enum BookingNotificationType implements NotificationType {
    CONFIRMED,
    CANCELLED,
    TICKET_ISSUED,
    PRICE_CHANGED,
    TICKET_ACTION_SUBMITTED,
    TICKET_ACTION_QUOTED,
    TICKET_ACTION_REJECTED,
    TICKET_ACTION_CONFIRMED,
    TICKET_ACTION_PROCESSING,
    TICKET_ACTION_COMPLETED,
    TICKET_ACTION_FAILED;

    @Override public String getCode() { return "BOOKING_" + name(); }
    @Override public NotificationCategory getCategory() { return NotificationCategory.BOOKING; }
}
