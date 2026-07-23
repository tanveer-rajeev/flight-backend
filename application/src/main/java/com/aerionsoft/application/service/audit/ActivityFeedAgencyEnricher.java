package com.aerionsoft.application.service.audit;

import com.aerionsoft.application.dto.audit.ActivityFeedAgencyInfo;
import com.aerionsoft.application.dto.audit.ActivityFeedDetailsInfo;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.audit.ActivityLog;
import com.aerionsoft.application.repository.booking.BookingRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ActivityFeedAgencyEnricher {

    private final ActivityAgencyContextSupport agencyContextSupport;
    private final BookingRepository bookingRepository;

    public ActivityFeedAgencyEnricher(
            ActivityAgencyContextSupport agencyContextSupport,
            BookingRepository bookingRepository) {
        this.agencyContextSupport = agencyContextSupport;
        this.bookingRepository = bookingRepository;
    }

    public Map<Long, ActivityFeedAgencyInfo> resolveAgencies(List<ActivityLog> logs, Map<Long, Map<String, Object>> metadataByLogId) {
        Map<Long, Long> bookingIdByLogId = new HashMap<>();
        Map<Long, Long> userIdByLogId = new HashMap<>();
        Map<Long, Long> businessIdByLogId = new HashMap<>();

        for (ActivityLog log : logs) {
            Map<String, Object> metadata = metadataByLogId.get(log.getId());
            if (metadata == null) {
                continue;
            }
            if (agencyContextSupport.snapshotFromMetadata(metadata).isPresent()) {
                continue;
            }
            Long bookingId = firstLong(metadata.get("bookingId"),
                    "BOOKING".equalsIgnoreCase(log.getResourceType()) ? parseLong(log.getResourceId()) : null);
            if (bookingId != null) {
                bookingIdByLogId.put(log.getId(), bookingId);
            }
            Long userId = firstLong(metadata.get("ownerUserId"), metadata.get("targetUserId"));
            if (userId != null) {
                userIdByLogId.put(log.getId(), userId);
            }
            Long businessId = firstLong(metadata.get("businessId"),
                    "BUSINESS".equalsIgnoreCase(log.getResourceType()) ? parseLong(log.getResourceId()) : null);
            if (businessId != null) {
                businessIdByLogId.put(log.getId(), businessId);
            }
        }

        Map<Long, Booking> bookingsById = loadBookings(bookingIdByLogId.values());
        Map<Long, ActivityFeedAgencyInfo> result = new HashMap<>();

        for (ActivityLog log : logs) {
            Map<String, Object> metadata = metadataByLogId.getOrDefault(log.getId(), Map.of());
            agencyContextSupport.snapshotFromMetadata(metadata)
                    .map(this::toAgencyInfo)
                    .ifPresent(info -> result.put(log.getId(), info));
        }

        for (Map.Entry<Long, Long> entry : bookingIdByLogId.entrySet()) {
            if (result.containsKey(entry.getKey())) {
                continue;
            }
            Booking booking = bookingsById.get(entry.getValue());
            if (booking == null || booking.getCreatedBy() == null) {
                continue;
            }
            agencyContextSupport.resolveSnapshot(booking.getCreatedBy().getId())
                    .map(this::toAgencyInfo)
                    .ifPresent(info -> result.put(entry.getKey(), info));
        }

        for (Map.Entry<Long, Long> entry : userIdByLogId.entrySet()) {
            if (result.containsKey(entry.getKey())) {
                continue;
            }
            agencyContextSupport.resolveSnapshot(entry.getValue())
                    .map(this::toAgencyInfo)
                    .ifPresent(info -> result.put(entry.getKey(), info));
        }

        for (Map.Entry<Long, Long> entry : businessIdByLogId.entrySet()) {
            if (result.containsKey(entry.getKey())) {
                continue;
            }
            agencyContextSupport.resolveFromBusinessId(entry.getValue())
                    .map(this::toAgencyInfo)
                    .ifPresent(info -> result.put(entry.getKey(), info));
        }

        return result;
    }

    public ActivityFeedDetailsInfo resolveDetails(Map<String, Object> metadata, ActivityLog log) {
        if (metadata == null) {
            metadata = Map.of();
        }
        BigDecimal amount = decimalVal(metadata.get("quoteTotalAmount"));
        if (amount == null) {
            amount = decimalVal(metadata.get("reissueChargeAmountUsd"));
        }
        if (amount == null) {
            amount = decimalVal(metadata.get("amount"));
        }

        return ActivityFeedDetailsInfo.builder()
                .bookingId(firstLong(metadata.get("bookingId"),
                        "BOOKING".equalsIgnoreCase(log.getResourceType()) ? parseLong(log.getResourceId()) : null))
                .pnr(stringVal(metadata.get("pnr")))
                .ticketNo(stringVal(metadata.get("ticketNo")))
                .bookingReference(stringVal(metadata.get("bookingReference")))
                .ticketActionRequestId(firstLong(metadata.get("ticketActionRequestId"),
                        "TICKET_ACTION_REQUEST".equalsIgnoreCase(log.getResourceType())
                                ? parseLong(log.getResourceId()) : null))
                .ticketActionType(stringVal(metadata.get("ticketActionType")))
                .ticketActionStatus(stringVal(metadata.get("status")))
                .amount(amount)
                .currency(firstString(metadata.get("quoteCurrency"), metadata.get("currency"), metadata.get("agencyCurrency")))
                .oldStatus(stringVal(metadata.get("oldStatus")))
                .newStatus(stringVal(metadata.get("newStatus")))
                .walletDepositId("WALLET_DEPOSIT".equalsIgnoreCase(log.getResourceType())
                        ? parseLong(log.getResourceId()) : null)
                .businessId(firstLong(metadata.get("businessId"),
                        "BUSINESS".equalsIgnoreCase(log.getResourceType()) ? parseLong(log.getResourceId()) : null))
                .build();
    }

    private Map<Long, Booking> loadBookings(Iterable<Long> bookingIds) {
        Set<Long> ids = new HashSet<>();
        bookingIds.forEach(id -> {
            if (id != null) {
                ids.add(id);
            }
        });
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Booking> map = new HashMap<>();
        bookingRepository.findAllById(ids).forEach(booking -> map.put(booking.getId(), booking));
        return map;
    }

    private ActivityFeedAgencyInfo toAgencyInfo(ActivityAgencyContextSupport.AgencySnapshot snapshot) {
        return ActivityFeedAgencyInfo.builder()
                .businessId(snapshot.businessId())
                .agencyUserId(snapshot.agencyUserId())
                .agencyName(snapshot.agencyName())
                .agencyEmail(snapshot.agencyEmail())
                .agencyPhone(snapshot.agencyPhone())
                .agencyCurrency(snapshot.agencyCurrency())
                .ownerUserId(snapshot.ownerUserId())
                .ownerUserName(snapshot.ownerUserName())
                .ownerUserEmail(snapshot.ownerUserEmail())
                .build();
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long firstLong(Object... values) {
        for (Object value : values) {
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value != null) {
                Long parsed = parseLong(String.valueOf(value));
                if (parsed != null) {
                    return parsed;
                }
            }
        }
        return null;
    }

    private static String firstString(Object... values) {
        for (Object value : values) {
            String text = stringVal(value);
            if (text != null && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private static String stringVal(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private static BigDecimal decimalVal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
