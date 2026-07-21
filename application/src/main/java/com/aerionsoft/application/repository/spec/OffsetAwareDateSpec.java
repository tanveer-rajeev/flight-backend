package com.aerionsoft.application.repository.spec;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.aerionsoft.application.context.UserTimezoneContext;
import com.aerionsoft.application.util.FilterRangeUtil;
import com.aerionsoft.application.util.UserTimezoneUtil;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

public final class OffsetAwareDateSpec {

    private OffsetAwareDateSpec() {}

    public static <T> Specification<T> createdAtInUserRange(
            LocalDate from,
            LocalDate to,
            String createdAtField,
            String createdTimeOffsetField
    ) {
        return createdAtInUserRange(from, to, createdAtField, createdTimeOffsetField, null);
    }

    public static <T> Specification<T> createdAtInUserRange(
            LocalDate from,
            LocalDate to,
            String createdAtField,
            String createdTimeOffsetField,
            String fallbackOffsetField
    ) {
        FilterRangeUtil.InstantRange range = FilterRangeUtil.userDateRange(from, to);
        if (range.start() == null && range.endExclusive() == null) {
            return null;
        }

        return (root, query, cb) -> {
            Expression<Timestamp> instantExpr = toInstantExpression(
                    root, cb, createdAtField, createdTimeOffsetField, fallbackOffsetField);
            List<Predicate> predicates = new ArrayList<>();

            if (range.start() != null) {
                predicates.add(cb.greaterThanOrEqualTo(instantExpr, Timestamp.from(range.start())));
            }
            if (range.endExclusive() != null) {
                predicates.add(cb.lessThan(instantExpr, Timestamp.from(range.endExclusive())));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static <T> Specification<T> createdAtOnUserDate(
            LocalDate date,
            String createdAtField,
            String createdTimeOffsetField
    ) {
        return createdAtOnUserDate(date, createdAtField, createdTimeOffsetField, null);
    }

    public static <T> Specification<T> createdAtOnUserDate(
            LocalDate date,
            String createdAtField,
            String createdTimeOffsetField,
            String fallbackOffsetField
    ) {
        if (date == null) {
            return null;
        }

        FilterRangeUtil.InstantRange range = FilterRangeUtil.userSingleDate(date);
        return (root, query, cb) -> {
            Expression<Timestamp> instantExpr = toInstantExpression(
                    root, cb, createdAtField, createdTimeOffsetField, fallbackOffsetField);
            return cb.and(
                    cb.greaterThanOrEqualTo(instantExpr, Timestamp.from(range.start())),
                    cb.lessThan(instantExpr, Timestamp.from(range.endExclusive()))
            );
        };
    }

    public static <T> Specification<T> createdAtInInstantRange(
            Instant startInclusive,
            Instant endExclusive,
            String createdAtField,
            String createdTimeOffsetField
    ) {
        return createdAtInInstantRange(
                startInclusive, endExclusive, createdAtField, createdTimeOffsetField, null);
    }

    public static <T> Specification<T> createdAtInInstantRange(
            Instant startInclusive,
            Instant endExclusive,
            String createdAtField,
            String createdTimeOffsetField,
            String fallbackOffsetField
    ) {
        if (startInclusive == null && endExclusive == null) {
            return null;
        }

        return (root, query, cb) -> {
            Expression<Timestamp> instantExpr = toInstantExpression(
                    root, cb, createdAtField, createdTimeOffsetField, fallbackOffsetField);
            List<Predicate> predicates = new ArrayList<>();

            if (startInclusive != null) {
                predicates.add(cb.greaterThanOrEqualTo(instantExpr, Timestamp.from(startInclusive)));
            }
            if (endExclusive != null) {
                predicates.add(cb.lessThan(instantExpr, Timestamp.from(endExclusive)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** User-local {@code LocalDateTime} bounds (e.g. activity/error log filters). */
    public static <T> Specification<T> createdAtFromUserLocalDateTimes(
            LocalDateTime fromInclusive,
            LocalDateTime toInclusive,
            String createdAtField,
            String createdTimeOffsetField
    ) {
        ZoneId zone = UserTimezoneContext.getZoneId();
        Instant start = fromInclusive != null ? fromInclusive.atZone(zone).toInstant() : null;
        Instant endExclusive = toInclusive != null ? toInclusive.atZone(zone).plusNanos(1).toInstant() : null;
        return createdAtInInstantRange(start, endExclusive, createdAtField, createdTimeOffsetField);
    }

    /** Rows stored as wall clock in a fixed zone (no per-row offset column). */
    public static <T> Specification<T> wallClockInUserDateRange(
            LocalDate from,
            LocalDate to,
            String timestampField,
            String assumedStoredOffset
    ) {
        FilterRangeUtil.InstantRange range = FilterRangeUtil.userDateRange(from, to);
        return wallClockInInstantRange(range.start(), range.endExclusive(), timestampField, assumedStoredOffset);
    }

    public static <T> Specification<T> wallClockInInstantRange(
            Instant startInclusive,
            Instant endExclusive,
            String timestampField,
            String assumedStoredOffset
    ) {
        if (startInclusive == null && endExclusive == null) {
            return null;
        }

        String offset = assumedStoredOffset != null ? assumedStoredOffset : UserTimezoneUtil.DEFAULT_OFFSET;
        return (root, query, cb) -> {
            Expression<Timestamp> instantExpr = cb.function(
                    "timezone",
                    Timestamp.class,
                    cb.literal(offset),
                    root.get(timestampField)
            );
            List<Predicate> predicates = new ArrayList<>();

            if (startInclusive != null) {
                predicates.add(cb.greaterThanOrEqualTo(instantExpr, Timestamp.from(startInclusive)));
            }
            if (endExclusive != null) {
                predicates.add(cb.lessThan(instantExpr, Timestamp.from(endExclusive)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static <T> Expression<Timestamp> toInstantExpression(
            Root<T> root,
            CriteriaBuilder cb,
            String createdAtField,
            String createdTimeOffsetField,
            String fallbackOffsetField
    ) {
        Expression<String> offsetExpr;
        if (fallbackOffsetField != null) {
            offsetExpr = cb.<String>coalesce()
                    .value(root.get(createdTimeOffsetField))
                    .value(root.get(fallbackOffsetField))
                    .value(cb.literal(UserTimezoneUtil.DEFAULT_OFFSET));
        } else {
            offsetExpr = cb.coalesce(
                    root.get(createdTimeOffsetField),
                    cb.literal(UserTimezoneUtil.DEFAULT_OFFSET)
            );
        }
        return cb.function(
                "timezone",
                Timestamp.class,
                offsetExpr,
                root.get(createdAtField)
        );
    }
}
