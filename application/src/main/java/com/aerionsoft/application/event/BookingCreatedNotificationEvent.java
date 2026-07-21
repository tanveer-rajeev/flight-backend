package com.aerionsoft.application.event;

public record BookingCreatedNotificationEvent(
        Long userId,
        String userEmail,
        String userFullName,
        String bookingReference,
        String pnr,
        String currency,
        Long bookingId
) {
}
