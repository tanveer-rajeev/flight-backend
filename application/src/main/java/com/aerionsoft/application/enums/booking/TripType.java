package com.aerionsoft.application.enums.booking;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TripType {
    ONE_WAY("ONE_WAY"),
    ROUND_TRIP("ROUND_TRIP"),
    MULTI_CITY("MULTI_CITY");

    private final String value;

    TripType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @JsonCreator
    public static TripType fromValue(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase();
        return Arrays.stream(TripType.values())
                .filter(t -> t.value.equalsIgnoreCase(normalized) || t.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown TripType: " + value));
    }
}