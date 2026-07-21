package com.aerionsoft.application.event;

public record HoldToBookFailureEvent(
        Long bookingId,
        String errorMessage
) {
}
