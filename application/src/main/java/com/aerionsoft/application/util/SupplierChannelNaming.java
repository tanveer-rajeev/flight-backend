package com.aerionsoft.application.util;

import com.aerionsoft.application.enums.booking.Provider;

import java.util.Locale;

public final class SupplierChannelNaming {

    private SupplierChannelNaming() {
    }

    /**
     * Normalizes booking channel (e.g. {@code galileo-bd}) to supplier name ({@code GALILEO_BD}).
     */
    public static String toSupplierName(String channel, Provider provider) {
        if (channel != null && !channel.isBlank()) {
            return channel.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        }
        return provider != null ? provider.name() : "OTHERS";
    }
}
