package com.aerionsoft.application.service.sabre;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.dto.sabre.DailyTicketSegmentCountApiResponse;
import com.aerionsoft.application.dto.sabre.DailyTicketSegmentListResponse;
import com.aerionsoft.application.dto.sabre.DailyTicketSegmentReloadResponse;
import com.aerionsoft.application.entity.DailyTicketSegment;
import com.aerionsoft.application.repository.flight.DailyTicketSegmentRepository;
import com.aerionsoft.application.util.TimestampMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DailyTicketSegmentService {

    private static final Logger log = LoggerFactory.getLogger(DailyTicketSegmentService.class);

    @Value("${core.booking.api.base-url}")
    private String coreApiBaseUrl;

    @Value("${flight_api_key}")
    private String apiKey;

    private static final String SEGMENT_COUNT_ENDPOINT = "/api/sabre/flights/daily-ticket-issued-segment-count";

    private final WebClient webClient;
    private final DailyTicketSegmentRepository repository;
    private final TimestampMapper timestampMapper;

    public DailyTicketSegmentService(WebClient insecureWebClient,
                                     DailyTicketSegmentRepository repository,
                                     TimestampMapper timestampMapper) {
        this.webClient = insecureWebClient;
        this.repository = repository;
        this.timestampMapper = timestampMapper;
    }

    /**
     * Fetches daily ticket issued segment count from external API for a given channel
     * and saves only new (non-duplicate) PNRs for today's date.
     */
    public DailyTicketSegmentReloadResponse fetchAndSave(String channel) {
        LocalDate today = LocalDate.now();
        String sessionId = UUID.randomUUID().toString().replace("-", "");

        log.info("[DailyTicketSegment] Fetching segment counts for channel={}, date={}", channel, today);

        DailyTicketSegmentCountApiResponse apiResponse;
        try {
            apiResponse = webClient.get()
                    .uri(coreApiBaseUrl + SEGMENT_COUNT_ENDPOINT
                            + "?sessionId=" + sessionId
                            + "&channel=" + channel)
                    .header("x-api-key", apiKey)
                    .retrieve()
                    .bodyToMono(DailyTicketSegmentCountApiResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("[DailyTicketSegment] Failed to fetch from API for channel={}: {}", channel, e.getMessage());
            return DailyTicketSegmentReloadResponse.builder()
                    .success(false)
                    .message("Failed to fetch from API: " + e.getMessage())
                    .date(today.toString())
                    .channel(channel)
                    .build();
        }

        if (apiResponse == null || !apiResponse.isSuccess()
                || apiResponse.getPnrSegmentCounts() == null
                || apiResponse.getPnrSegmentCounts().isEmpty()) {
            log.warn("[DailyTicketSegment] No data returned from API for channel={}", channel);

            // Still return whatever is already stored for today
            DailyTicketSegmentListResponse existing = getByDateAndChannel(today, channel);
            return DailyTicketSegmentReloadResponse.builder()
                    .success(true)
                    .message("No data returned from API")
                    .date(today.toString())
                    .channel(channel)
                    .totalFromApi(0)
                    .newRecordsSaved(0)
                    .skippedDuplicates(0)
                    .totalStoredForDay(existing.getTotalRecords())
                    .records(existing.getRecords())
                    .build();
        }

        // Determine the date from the API response (may differ from today in edge cases)
        LocalDate recordDate = today;
        if (apiResponse.getDate() != null && !apiResponse.getDate().isBlank()) {
            try {
                recordDate = LocalDate.parse(apiResponse.getDate());
            } catch (Exception ignored) {
                log.warn("[DailyTicketSegment] Could not parse date '{}', using today", apiResponse.getDate());
            }
        }

        // Fetch already-stored PNRs for this date+channel to detect duplicates
        Set<String> existingPnrs = repository.findExistingPnrsByDateAndChannel(recordDate, channel);

        List<DailyTicketSegment> toSave = new ArrayList<>();
        int skipped = 0;

        for (DailyTicketSegmentCountApiResponse.PnrSegmentCount item : apiResponse.getPnrSegmentCounts()) {
            if (existingPnrs.contains(item.getPnr())) {
                skipped++;
                log.debug("[DailyTicketSegment] Skipping duplicate PNR={} for date={}, channel={}", item.getPnr(), recordDate, channel);
                continue;
            }
            toSave.add(DailyTicketSegment.builder()
                    .pnr(item.getPnr())
                    .segmentCount(item.getSegmentCount())
                    .channel(channel)
                    .date(recordDate)
                    .createdAt(UserDateTimeUtil.now())
                    .build());
        }

        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
            log.info("[DailyTicketSegment] Saved {} new records for channel={}, date={}", toSave.size(), channel, recordDate);
        }

        // Fetch full up-to-date list for this channel+date to include in the response
        DailyTicketSegmentListResponse current = getByDateAndChannel(recordDate, channel);

        return DailyTicketSegmentReloadResponse.builder()
                .success(true)
                .message("OK")
                .date(recordDate.toString())
                .channel(channel)
                .totalFromApi(apiResponse.getPnrSegmentCounts().size())
                .newRecordsSaved(toSave.size())
                .skippedDuplicates(skipped)
                .totalStoredForDay(current.getTotalRecords())
                .records(current.getRecords())
                .build();
    }

    /**
     * Returns all stored segment records for a given date (all channels).
     */
    public DailyTicketSegmentListResponse getByDate(LocalDate date) {
        List<DailyTicketSegment> records = repository.findByDateOrderByCreatedAtDesc(date);
        return buildListResponse(records, date, null);
    }

    /**
     * Returns stored segment records filtered by date and channel.
     */
    public DailyTicketSegmentListResponse getByDateAndChannel(LocalDate date, String channel) {
        List<DailyTicketSegment> records = repository.findByDateAndChannelOrderByCreatedAtDesc(date, channel);
        return buildListResponse(records, date, channel);
    }

    private DailyTicketSegmentListResponse buildListResponse(List<DailyTicketSegment> records, LocalDate date, String channel) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        List<DailyTicketSegmentListResponse.SegmentRecord> segmentRecords = records.stream()
                .map(r -> DailyTicketSegmentListResponse.SegmentRecord.builder()
                        .id(r.getId())
                        .pnr(r.getPnr())
                        .segmentCount(r.getSegmentCount())
                        .channel(r.getChannel())
                        .date(r.getDate())
                        .createdAt(timestampMapper.toRequestUserTimeFormatted(r.getCreatedAt(), r.getCreatedTimeOffset(), dtf))
                        .build())
                .toList();

        int totalSegments = records.stream()
                .mapToInt(r -> r.getSegmentCount() != null ? r.getSegmentCount() : 0)
                .sum();

        return DailyTicketSegmentListResponse.builder()
                .success(true)
                .message("OK")
                .date(date)
                .totalRecords(records.size())
                .totalSegments(totalSegments)
                .records(segmentRecords)
                .build();
    }
}

