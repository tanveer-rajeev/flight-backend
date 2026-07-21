package com.aerionsoft.application.enums.group;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum GroupTicketType {
    GROUP("GROUP"),
    UMRAH("UMRAH"),
    A2A("A2A");

    private final String value;

    GroupTicketType(String value) {
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
    public static GroupTicketType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return Arrays.stream(GroupTicketType.values())
                .filter(t -> t.value.equalsIgnoreCase(normalized) || t.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid group ticket type: " + value + ". Allowed values: GROUP, UMRAH, A2A"));
    }
}
