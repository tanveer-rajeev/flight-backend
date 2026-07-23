package com.aerionsoft.notification.factory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderInterpolator {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");

    private PlaceholderInterpolator() {
    }

    public static String interpolate(String template, Map<String, Object> metadata) {
        if (template == null) {
            return "";
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = metadata.get(key);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value != null ? value.toString() : ""));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
