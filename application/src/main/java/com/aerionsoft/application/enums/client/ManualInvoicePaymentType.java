package com.aerionsoft.application.enums.client;

public enum ManualInvoicePaymentType {
    BANK,
    CASH,
    CARD,
    OTHERS;

    public static ManualInvoicePaymentType fromValue(String value) {
        if (value == null) return null;
        return ManualInvoicePaymentType.valueOf(value.trim().toUpperCase());
    }
}
