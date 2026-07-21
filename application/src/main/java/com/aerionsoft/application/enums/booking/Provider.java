package com.aerionsoft.application.enums.booking;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Provider {
    TBO("TBO"),
    SABRE("SABRE"),
    ARABIA("ARABIA"),
    AKIJ("AKIJ"),
    USBANGLA("USBANGLA"),
    VERTEIL("VERTEIL"),
    GROUP("GROUP"),
    USBANGLAAPI("USBANGLAAPI"),
    FLYDUBAI("FLYDUBAI"),
    GALILEO("GALILEO"),
    OTHERS("OTHERS");


    private final String value;

    Provider(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Provider getByName(String value) {
        for (Provider provider : Provider.values()) {
            if (provider.value.equalsIgnoreCase(value) || provider.name().equalsIgnoreCase(value)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Invalid Provider value: " + value);
    }
}
