package com.aerionsoft.application.util;

public final class EmailUtils {

    private EmailUtils() {
    }

    public static String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
