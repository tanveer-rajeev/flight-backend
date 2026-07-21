package com.aerionsoft.application.enums.client;

public enum InvoiceType {
    FLIGHT,
    VISA,
    TOUR,
    OTHER;

    public static InvoiceType fromValue(String value) {
        if (value == null) return null;
        return InvoiceType.valueOf(value.trim().toUpperCase());
    }
}
