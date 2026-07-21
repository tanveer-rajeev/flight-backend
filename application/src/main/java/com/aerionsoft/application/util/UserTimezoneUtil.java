package com.aerionsoft.application.util;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UserTimezoneUtil {

    public static final String DEFAULT_OFFSET = "Asia/Dhaka";
    public static final ZoneId DEFAULT_ZONE = ZoneId.of(DEFAULT_OFFSET);

  private static final Logger log = LoggerFactory.getLogger(UserTimezoneUtil.class);
    private static final Pattern OFFSET_PATTERN = Pattern.compile("^([+-])(\\d{1,2}):(\\d{2})$");

    private UserTimezoneUtil() {}

    public static ZoneId resolve(String userTimeOffset) {
        if (userTimeOffset == null || userTimeOffset.isBlank()) {
            return DEFAULT_ZONE;
        }

        String normalized = userTimeOffset.replaceAll("\\s+", "").trim();

        ZoneOffset numericOffset = parseNumericOffset(normalized);
        if (numericOffset != null) {
            return numericOffset;
        }

        try {
            return ZoneId.of(normalized);
        } catch (DateTimeException ex) {
            log.warn("Invalid user time offset '{}', falling back to {}", userTimeOffset, DEFAULT_OFFSET);
            return DEFAULT_ZONE;
        }
    }

    public static ZoneOffset parseNumericOffset(String offset) {
        if (offset == null || offset.isBlank()) {
            return null;
        }

        Matcher matcher = OFFSET_PATTERN.matcher(offset);
        if (!matcher.matches()) {
            return null;
        }

        String sign = matcher.group(1);
        int hours = Integer.parseInt(matcher.group(2));
        int minutes = Integer.parseInt(matcher.group(3));

        if (hours > 18 || minutes > 59) {
            return null;
        }

        return ZoneOffset.of(String.format("%s%02d:%02d", sign, hours, minutes));
    }

    public static String normalizeOffsetString(String userTimeOffset) {
        if (userTimeOffset == null || userTimeOffset.isBlank()) {
            return DEFAULT_OFFSET;
        }
        return userTimeOffset.trim();
    }
}
