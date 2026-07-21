package com.aerionsoft.application.enums.wallet;

public enum PaymentType {
    DEPOSIT,
    BOOKING,
    WITHDRAW;

    public static PaymentType fromValue(String value) {
        if (value == null) return null;
        return PaymentType.valueOf(value.trim().toUpperCase());
    }
}
