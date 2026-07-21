package com.aerionsoft.application.enums.flight;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How route, airline, and cabin class filters are evaluated on a markup rule.
 */
public enum MarkupFilterMode {
    /** Each of routes, airlineCode, and bookingCodes is checked independently (AND). */
    INDIVIDUAL("INDIVIDUAL"),
    /** Only {@code combinedConditions} rows are used; individual route/airline/bookingCode fields are ignored. */
    COMBINED("COMBINED");

    private final String value;

    MarkupFilterMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static MarkupFilterMode fromValue(String value) {
        if (value == null || value.isBlank()) {
            return INDIVIDUAL;
        }
        for (MarkupFilterMode mode : values()) {
            if (mode.value.equalsIgnoreCase(value) || mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Invalid MarkupFilterMode value: " + value);
    }
}
