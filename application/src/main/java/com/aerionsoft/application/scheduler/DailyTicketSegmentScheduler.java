package com.aerionsoft.application.scheduler;

import com.aerionsoft.application.service.sabre.DailyTicketSegmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Scheduler that runs daily at 11:55 PM (Asia/Dhaka) to fetch and store
 * the daily issued-ticket segment counts from the Sabre API for all configured channels.
 */
@Component
public class DailyTicketSegmentScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyTicketSegmentScheduler.class);

    private final DailyTicketSegmentService service;

    @Value("${sabre.daily.segment.channels:s-bd}")
    private String channelsConfig;

    public DailyTicketSegmentScheduler(DailyTicketSegmentService service) {
        this.service = service;
    }

    /**
     * Runs every day at 11:55 PM Asia/Dhaka time.
     * Fetches and stores daily ticket segment counts for all configured channels.
     */
    @Scheduled(cron = "0 55 23 * * ?", zone = "Asia/Dhaka")
    public void fetchDailySegmentCounts() {
        List<String> channels = Arrays.stream(channelsConfig.split(","))
                .map(String::trim)
                .filter(c -> !c.isBlank())
                .toList();

        log.info("[DailyTicketSegmentScheduler] Starting daily fetch for channels: {}", channels);

        for (String channel : channels) {
            try {
                var result = service.fetchAndSave(channel);
                log.info("[DailyTicketSegmentScheduler] channel={} → saved={}, skipped={}, total={}",
                        channel, result.getNewRecordsSaved(), result.getSkippedDuplicates(), result.getTotalFromApi());
            } catch (Exception e) {
                log.error("[DailyTicketSegmentScheduler] Error processing channel={}: {}", channel, e.getMessage(), e);
            }
        }

        log.info("[DailyTicketSegmentScheduler] Daily fetch complete.");
    }
}

