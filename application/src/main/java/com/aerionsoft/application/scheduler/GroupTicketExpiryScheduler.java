package com.aerionsoft.application.scheduler;

import com.aerionsoft.application.service.admin.GroupTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that auto-expires group tickets whose booking window has ended.
 *
 * A group ticket is considered expired when its {@code bookingEnds} date
 * is strictly before today's date and its status is not already "EXPIRED".
 *
 * Runs every day at midnight (00:00:00 server time).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroupTicketExpiryScheduler {

    private final GroupTicketService groupTicketService;

    /**
     * Daily midnight job – expire group tickets whose bookingEnds < today.
     * Cron: "0 0 0 * * ?" = every day at 00:00:00
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void autoExpireGroupTickets() {
        log.info("=== GroupTicketExpiryScheduler: starting auto-expiry check ===");
        try {
            int count = groupTicketService.expireGroupTickets();
            log.info("=== GroupTicketExpiryScheduler: completed – {} ticket(s) expired ===", count);
        } catch (Exception e) {
            log.error("=== GroupTicketExpiryScheduler: error during auto-expiry ===", e);
        }
    }

    /**
     * Manual trigger endpoint support – can be called from a controller for on-demand runs.
     *
     * @return summary message
     */
    public String triggerManualExpiry() {
        log.info("GroupTicketExpiryScheduler: manual trigger invoked");
        int count = groupTicketService.expireGroupTickets();
        return count + " group ticket(s) expired.";
    }
}

