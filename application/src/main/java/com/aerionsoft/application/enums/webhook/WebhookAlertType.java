package com.aerionsoft.application.enums.webhook;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Outbound alert event types. Add new values here and in admin meta endpoint when introducing new cases.
 */
public enum WebhookAlertType {
    TICKETED_BOOKING_POST_PROCESS_FAILED(
            "TICKETED_BOOKING_POST_PROCESS_FAILED",
            "Ticket issued but post-processing failed"
    ),
    BOOKING_CREATE_CORE_FAILED(
            "BOOKING_CREATE_CORE_FAILED",
            "Booking create core API failed"
    ),
    HOLD_TO_BOOK_CORE_FAILED(
            "HOLD_TO_BOOK_CORE_FAILED",
            "Hold-to-book core API failed"
    );

    private final String value;
    private final String label;

    WebhookAlertType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static WebhookAlertType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Webhook alert type is required");
        }
        for (WebhookAlertType type : values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid webhook alert type: " + value);
    }
}
