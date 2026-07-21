package com.aerionsoft.application.enums.client;

public enum InvoiceStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public static InvoiceStatus fromValue(String value) {
        if (value == null) return null;
        return InvoiceStatus.valueOf(value.trim().toUpperCase());
    }
}
