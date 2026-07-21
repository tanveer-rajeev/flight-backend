package com.aerionsoft.application.enums.booking;

import lombok.Getter;

@Getter
public enum ServiceProviderEnum {

    ADMIN("admin"),
    USER("user");

    private final String provider;

    ServiceProviderEnum(String provider) {
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }

    public static ServiceProviderEnum fromString(String provider) {
        for (ServiceProviderEnum value : ServiceProviderEnum.values()) {
            if (value.provider.equalsIgnoreCase(provider)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown service provider: " + provider);
    }

    public static boolean isValidProvider(String provider) {
        for (ServiceProviderEnum value : ServiceProviderEnum.values()) {
            if (value.provider.equalsIgnoreCase(provider)) {
                return true;
            }
        }
        return false;
    }

}
