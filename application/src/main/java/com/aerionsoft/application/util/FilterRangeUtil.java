package com.aerionsoft.application.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.aerionsoft.application.context.UserTimezoneContext;

public final class FilterRangeUtil {

    private FilterRangeUtil() {}

    public record InstantRange(Instant start, Instant endExclusive) {}

    public static InstantRange userDateRange(LocalDate from, LocalDate to) {
        ZoneId zone = UserTimezoneContext.getZoneId();
        Instant start = from != null ? from.atStartOfDay(zone).toInstant() : null;
        Instant end = to != null ? to.plusDays(1).atStartOfDay(zone).toInstant() : null;
        return new InstantRange(start, end);
    }

    public static InstantRange userSingleDate(LocalDate date) {
        if (date == null) {
            return new InstantRange(null, null);
        }
        ZoneId zone = UserTimezoneContext.getZoneId();
        return new InstantRange(
                date.atStartOfDay(zone).toInstant(),
                date.plusDays(1).atStartOfDay(zone).toInstant()
        );
    }
}
