package com.aerionsoft.application.util;

import com.aerionsoft.application.entity.Booking.Booking;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

public final class PaymentDeadlineUtil {

    private static final DateTimeFormatter FLEXIBLE_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .toFormatter();

    private PaymentDeadlineUtil() {}

    /**
     * Resolves the payment deadline as a UTC instant.
     * <p>
     * Supports:
     * <ul>
     *   <li>UTC ISO instant ({@code 2026-06-03T17:59:00Z}) — Sabre/Galileo</li>
     *   <li>Offset datetime ({@code 2026-06-03T17:59:00+04:00})</li>
     *   <li>Local datetime + {@code bookedTimeOffset} (then {@code timeOffset}, then Asia/Dhaka) — LCC providers</li>
     * </ul>
     * Does not use {@code lastPaymentDateInSeconds}; the provider deadline string is the source of truth.
     */
    public static Instant resolveDeadlineInstant(Booking booking) {
        String lastPaymentDate = booking.getLastPaymentDate();
        if (lastPaymentDate == null || lastPaymentDate.isBlank()) {
            throw new DateTimeParseException(
                    "Missing lastPaymentDate for booking " + booking.getId(),
                    "",
                    0);
        }

        Instant parsed = tryParseDeadlineString(lastPaymentDate.trim(), booking);
        if (parsed != null) {
            return parsed;
        }

        throw new DateTimeParseException(
                "Unable to parse payment deadline for booking " + booking.getId(),
                lastPaymentDate,
                0);
    }

    private static Instant tryParseDeadlineString(String value, Booking booking) {
        try {
            if (value.endsWith("Z")) {
                return Instant.parse(value);
            }
            if (value.matches(".*[+-]\\d{2}:\\d{2}$")) {
                return OffsetDateTime.parse(value).toInstant();
            }
        } catch (DateTimeParseException ignored) {
            // fall through to local datetime + offset
        }

        ZoneOffset offset = resolveBookingOffset(booking);
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(value, FLEXIBLE_DATE_TIME_FORMATTER);
            return localDateTime.toInstant(offset);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static ZoneOffset resolveBookingOffset(Booking booking) {
        ZoneOffset offset = UserTimezoneUtil.parseNumericOffset(normalizeOffset(booking.getBookedTimeOffset()));
        if (offset != null) {
            return offset;
        }
        offset = UserTimezoneUtil.parseNumericOffset(normalizeOffset(booking.getTimeOffset()));
        if (offset != null) {
            return offset;
        }
        return UserTimezoneUtil.DEFAULT_ZONE.getRules().getOffset(Instant.now());
    }

    private static String normalizeOffset(String offset) {
        if (offset == null || offset.isBlank()) {
            return null;
        }
        return offset.replaceAll("\\s+", "").trim();
    }
}
