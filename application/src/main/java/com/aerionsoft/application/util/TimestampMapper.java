package com.aerionsoft.application.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.aerionsoft.application.context.UserTimezoneContext;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.HasCreatedUserTimestamp;
import com.aerionsoft.application.entity.HasUpdatedUserTimestamp;

import org.springframework.stereotype.Component;

@Component
public class TimestampMapper {

    public LocalDateTime toRequestUserTime(LocalDateTime storedAt, String storedOffset) {
        if (storedAt == null) {
            return null;
        }

        ZoneId sourceZone = UserTimezoneUtil.resolve(
                storedOffset != null ? storedOffset : UserTimezoneUtil.DEFAULT_OFFSET
        );
        ZoneId targetZone = UserTimezoneContext.getZoneId();

        return storedAt.atZone(sourceZone)
                .withZoneSameInstant(targetZone)
                .toLocalDateTime();
    }

    public LocalDateTime createdAt(HasCreatedUserTimestamp entity) {
        if (entity == null) {
            return null;
        }
        return toRequestUserTime(entity.getCreatedAt(), entity.getCreatedTimeOffset());
    }

    public LocalDateTime updatedAt(HasUpdatedUserTimestamp entity, String fallbackOffset) {
        if (entity == null) {
            return null;
        }
        String offset = entity.getUpdatedTimeOffset() != null
                ? entity.getUpdatedTimeOffset()
                : fallbackOffset;
        return toRequestUserTime(entity.getUpdatedAt(), offset);
    }

    public LocalDateTime updatedAt(HasCreatedUserTimestamp created, HasUpdatedUserTimestamp updated) {
        String fallback = created != null ? created.getCreatedTimeOffset() : null;
        return updatedAt(updated, fallback);
    }

    public String createdAtString(HasCreatedUserTimestamp entity) {
        LocalDateTime converted = createdAt(entity);
        return converted != null ? converted.toString() : null;
    }

    public String toRequestUserTimeString(LocalDateTime storedAt, String storedOffset) {
        LocalDateTime converted = toRequestUserTime(storedAt, storedOffset);
        return converted != null ? converted.toString() : null;
    }

    public String toRequestUserTimeFormatted(
            LocalDateTime storedAt,
            String storedOffset,
            DateTimeFormatter formatter
    ) {
        LocalDateTime converted = toRequestUserTime(storedAt, storedOffset);
        return converted != null ? converted.format(formatter) : null;
    }

    public boolean isInUserDateRange(LocalDateTime storedAt, String storedOffset, LocalDate from, LocalDate to) {
        if (storedAt == null) {
            return false;
        }
        FilterRangeUtil.InstantRange range = FilterRangeUtil.userDateRange(from, to);
        Instant instant = toStoredInstant(storedAt, storedOffset);
        if (instant == null) {
            return false;
        }
        if (range.start() != null && instant.isBefore(range.start())) {
            return false;
        }
        if (range.endExclusive() != null && !instant.isBefore(range.endExclusive())) {
            return false;
        }
        return true;
    }

    public String resolveStoredOffset(String primaryOffset, String fallbackOffset) {
        if (primaryOffset != null && !primaryOffset.isBlank()) {
            return primaryOffset.trim();
        }
        if (fallbackOffset != null && !fallbackOffset.isBlank()) {
            return fallbackOffset.trim();
        }
        return UserTimezoneUtil.DEFAULT_OFFSET;
    }

    public String bookingStoredOffset(Booking booking) {
        if (booking == null) {
            return UserTimezoneUtil.DEFAULT_OFFSET;
        }
        return resolveStoredOffset(booking.getCreatedTimeOffset(), booking.getTimeOffset());
    }

    public LocalDateTime bookingCreatedAt(Booking booking) {
        if (booking == null) {
            return null;
        }
        return toRequestUserTime(booking.getCreatedAt(), bookingStoredOffset(booking));
    }

    public boolean isBookingInUserDateRange(Booking booking, LocalDate from, LocalDate to) {
        if (booking == null) {
            return false;
        }
        return isInUserDateRange(booking.getCreatedAt(), bookingStoredOffset(booking), from, to);
    }

    /** Absolute instant for a DB {@code created_at} + {@code created_time_offset} pair. */
    public Instant toStoredInstant(LocalDateTime storedAt, String storedOffset) {
        if (storedAt == null) {
            return null;
        }
        ZoneId sourceZone = UserTimezoneUtil.resolve(
                storedOffset != null ? storedOffset : UserTimezoneUtil.DEFAULT_OFFSET
        );
        return storedAt.atZone(sourceZone).toInstant();
    }
}
