package com.aerionsoft.application.util;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Helper {

    /** Formats a monetary amount with exactly two digits after the decimal point. */
    public static String formatMoney(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }
    public static String randomCodeGenerator(int length) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int randomDigit = (int) (Math.random() * 10);
            code.append(randomDigit);
        }
        return code.toString();
    }

    public static List<Long> parseIds(String listOfIds) {
        if (listOfIds == null || listOfIds.isEmpty()) return null;
        try {
            return List.of(listOfIds.split(","))
                    .stream()
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            return null;
        }
    }


}
