package com.aerionsoft.application.context;

import java.time.ZoneId;

import com.aerionsoft.application.util.UserTimezoneUtil;

public final class UserTimezoneContext {

    private static final ThreadLocal<String> USER_TIME_OFFSET = new ThreadLocal<>();
    private static final ThreadLocal<ZoneId> ZONE_ID = new ThreadLocal<>();

    private UserTimezoneContext() {}

    public static void set(String userTimeOffset) {
        String normalized = UserTimezoneUtil.normalizeOffsetString(userTimeOffset);
        USER_TIME_OFFSET.set(normalized);
        ZONE_ID.set(UserTimezoneUtil.resolve(normalized));
    }

    public static void clear() {
        USER_TIME_OFFSET.remove();
        ZONE_ID.remove();
    }

    public static ZoneId getZoneId() {
        ZoneId zone = ZONE_ID.get();
        return zone != null ? zone : UserTimezoneUtil.DEFAULT_ZONE;
    }

    public static String getUserTimeOffset() {
        String offset = USER_TIME_OFFSET.get();
        return offset != null ? offset : UserTimezoneUtil.DEFAULT_OFFSET;
    }
}
