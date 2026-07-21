package com.aerionsoft.application.enums.booking;

import lombok.Getter;

@Getter
public enum BookingClass {

    ECONOMY(1, "Economy Class"),
    PREMIUM_ECONOMY(2, "Premium Economy"),
    BUSINESS(3, "Business Class"),
    FIRST(4, "First Class");

    private final int value;
    private final String displayName;

    BookingClass(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    // Get enum by value
    public static BookingClass fromValue(int value) {
        for (BookingClass bc : BookingClass.values()) {
            if (bc.value == value) {
                return bc;
            }
        }
        throw new IllegalArgumentException("Invalid booking class value: " + value);
    }

    @Override
    public String toString() {
        return displayName + " (" + value + ")";
    }
}
