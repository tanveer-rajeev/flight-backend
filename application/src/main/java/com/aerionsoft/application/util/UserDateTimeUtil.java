package com.aerionsoft.application.util;

import java.time.LocalDateTime;

import com.aerionsoft.application.context.UserTimezoneContext;

public final class UserDateTimeUtil {

    private UserDateTimeUtil() {}

    public static LocalDateTime now() {
        return LocalDateTime.now(UserTimezoneContext.getZoneId());
    }

    public static String currentOffset() {
        return UserTimezoneContext.getUserTimeOffset();
    }
}
