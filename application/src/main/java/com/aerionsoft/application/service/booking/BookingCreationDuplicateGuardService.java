package com.aerionsoft.application.service.booking;

import com.aerionsoft.application.dto.booking.BookingRequest;
import com.aerionsoft.application.dto.booking.SegmentRequest;
import com.aerionsoft.application.dto.traveller.TravellerRequest;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ServiceExceptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@Slf4j
public class BookingCreationDuplicateGuardService {

    static final String KEY_PREFIX = "booking:create:dedup:";
    static final Duration DEDUP_TTL = Duration.ofMinutes(3);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public BookingCreationDuplicateGuardService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Blocks duplicate booking submissions with the same payload for the same user.
     *
     * @return Redis lock key when acquired; empty string when Redis is unavailable (fail-open)
     */
    public String acquireOrThrow(BookingRequest req, Long userId) {
        String fingerprint = buildFingerprint(req, userId);
        String key = KEY_PREFIX + fingerprint;

        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(userId), DEDUP_TTL);
            if (Boolean.FALSE.equals(acquired)) {
                throw ServiceExceptions.duplicate(
                        "A booking with the same details is already in progress or was recently submitted. Please wait before trying again.");
            }
            return key;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Booking dedup check skipped because Redis is unavailable: {}", e.getMessage());
            return "";
        }
    }

    /** Releases the dedup lock so the user can retry after a failed booking attempt. */
    public void release(String lockKey) {
        if (lockKey == null || lockKey.isBlank()) {
            return;
        }
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.warn("Failed to release booking dedup lock {}: {}", lockKey, e.getMessage());
        }
    }

    String buildFingerprint(BookingRequest req, Long userId) {
        Map<String, Object> canonical = new TreeMap<>();
        canonical.put("userId", userId);
        canonical.put("resultIndex", normalize(req.getResultIndex()));
        canonical.put("bundleCode", normalize(req.getBundleCode()));
        canonical.put("providerName", req.getProviderName() != null ? req.getProviderName().name() : "");
        canonical.put("channel", normalize(req.getChannel()));
        canonical.put("bookType", req.getBookType() != null ? req.getBookType().name() : "");
        canonical.put("tripType", req.getTripType() != null ? req.getTripType().name() : "");
        canonical.put("type", req.getType() != null ? req.getType().name() : "");
        canonical.put("groupTicketType", normalize(req.getGroupTicketType()));

        List<Long> travellerIds = req.getTravellerIds() == null
                ? List.of()
                : req.getTravellerIds().stream().sorted().toList();
        canonical.put("travellerIds", travellerIds);
        canonical.put("itineraries", itineraryFingerprints(req.getItineraries()));
        canonical.put("segments", segmentFingerprints(req.getSegments()));

        try {
            return sha256(objectMapper.writeValueAsString(canonical));
        } catch (JsonProcessingException e) {
            throw ServiceExceptions.internal("Failed to fingerprint booking request", e);
        }
    }

    private static List<Map<String, String>> itineraryFingerprints(List<TravellerRequest> itineraries) {
        if (itineraries == null || itineraries.isEmpty()) {
            return List.of();
        }

        List<Map<String, String>> fingerprints = new ArrayList<>();
        for (TravellerRequest traveller : itineraries) {
            Map<String, String> entry = new TreeMap<>();
            entry.put("firstName", normalize(traveller.getFirstName()));
            entry.put("lastName", normalize(traveller.getLastName()));
            entry.put("dob", normalize(traveller.getDob()));
            entry.put("passportNo", normalize(traveller.getPassportNo()));
            entry.put("nationality", normalize(traveller.getNationality()));
            fingerprints.add(entry);
        }

        fingerprints.sort(Comparator
                .comparing((Map<String, String> entry) -> entry.get("passportNo"))
                .thenComparing(entry -> entry.get("dob"))
                .thenComparing(entry -> entry.get("firstName"))
                .thenComparing(entry -> entry.get("lastName")));
        return fingerprints;
    }

    private static List<String> segmentFingerprints(List<SegmentRequest> segments) {
        if (segments == null || segments.isEmpty()) {
            return List.of();
        }

        return segments.stream()
                .sorted(Comparator.comparing(
                        segment -> segment.getSegmentOrder() != null ? segment.getSegmentOrder() : Integer.MAX_VALUE))
                .map(segment -> {
                    String airlineCode = "";
                    String flightNumber = "";
                    if (segment.getAirline() != null) {
                        airlineCode = normalize(segment.getAirline().getAirlineCode());
                        flightNumber = normalize(segment.getAirline().getFlightNumber());
                    }

                    String depTime = "";
                    if (segment.getOrigin() != null) {
                        depTime = normalize(segment.getOrigin().getDepTime());
                    }

                    return airlineCode + flightNumber + "|" + depTime;
                })
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
