package com.aerionsoft.application.enums.common;

public enum Currency {
    BDT,
    INR,
    USD,
    AED,
    PKR,
    SAR,
    QAR;

    public static String fromCode(String code) {
        if (code == null) {
            return USD.name();
        }
        try {
            return Currency.valueOf(code.toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            return USD.name();
        }
    }

    public static Currency getIndexFromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        try {
            return Currency.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null; // invalid currency
        }
    }
}

