package com.aerionsoft.application.enums.access;

public enum PermissionType {
    ADMIN,
    AGENCY,
    USER,
    COMMON;

    public static PermissionType fromValue(String value) {
        if (value == null) return null;
        return PermissionType.valueOf(value.trim().toUpperCase());
    }
}
