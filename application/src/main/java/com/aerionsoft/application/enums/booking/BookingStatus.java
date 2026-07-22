package com.aerionsoft.application.enums.booking;

public enum BookingStatus {
    ALL,        // special: for search only
    PROCESS,    // being processed
    PNR,        // PNR assigned but not confirmed
    CONFIRMED,
    COMPLETED,
    CANCELLED,
    FAILED,
    BOOK,
    TICKETED,
    REJECTED,
    VALIDATION_PROCESS,
    VALIDATION_SUCCESS,
    VALIDATION_FAILED,
    VALIDATION_PRICE_CHANGED,  // price changed during validation
    ON_HOLD,
    REPRICE,// booking is on hold
    VOID,
    TICKET_CANCELLED,
    REFUND,
    REISSUE,
    TICKET_ISSUED,

    // Pre-booking flight journey steps (stored in booking_timeline)
    SEARCH,
    SEARCH_FAILED,
    BUNDLE_VALIDATION_SUCCESS,
    BUNDLE_VALIDATION_FAILED,
    ADD_TO_CART,
    ADD_TO_CART_FAILED;
}
