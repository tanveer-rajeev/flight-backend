package com.aerionsoft.application.enums.common;

public enum NGeniusActionType {
    AUTH,
    SALE,
    PURCHASE;

    public static NGeniusActionType fromValue(String value) {
        if (value == null) return null;
        return NGeniusActionType.valueOf(value.trim().toUpperCase());
    }
}