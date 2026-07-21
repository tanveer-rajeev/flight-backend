package com.aerionsoft.application.enums.access;

public enum PermissionModule {
    API,
    CUSTOM,
    MENU;

    public static PermissionModule fromValue(String value) {
        if (value == null) return null;
        return PermissionModule.valueOf(value.trim().toUpperCase());
    }
}
