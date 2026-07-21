package com.aerionsoft.application.enums.booking;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BookType {
    HOLD, BOOK, MANUAL;



    @JsonCreator
    public static BookType fromString(String key) {
        if (key == null) return null;
        return switch (key.trim().toUpperCase()) {   // accept legacy/incorrect value
            case "BOOK", "FLIGHT", "RESERVE" -> BOOK;
            case "HOLD", "HOLDING" -> HOLD;
            case "MANUAL" -> MANUAL;
            default -> throw new IllegalArgumentException("Unknown BookType: " + key);
        };
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
