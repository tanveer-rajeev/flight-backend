package com.aerionsoft.application.scheduler;

import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.repository.booking.BookingRepository;
import com.aerionsoft.application.service.booking.TicketingDeadlineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Scheduled task to check and update Sabre and Verteil bookings without ticketing deadline.
 * Runs periodically to ensure all supported provider bookings have lastPaymentDate populated.
 * This serves as a backup mechanism for the async update process.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketingDeadlineScheduler {

    private final BookingRepository bookingRepository;
    private final TicketingDeadlineService ticketingDeadlineService;

    /**
     * Runs every hour to check and update Sabre and Verteil bookings without lastPaymentDate.
     * Cron: "0 0 * * * ?" = Every hour at minute 0
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void updateMissingTicketingDeadlines() {
        log.info("Starting scheduled ticketing deadline update job (Sabre + Verteil + Galileo)...");

        try {
            List<Booking> bookingsWithoutDeadline = findBookingsWithoutDeadline();

            if (bookingsWithoutDeadline.isEmpty()) {
                log.info("No bookings found that need ticketing deadline update");
                return;
            }

            log.info("Found {} booking(s) without ticketing deadline. Processing...",
                    bookingsWithoutDeadline.size());

            int successCount = 0;
            int failureCount = 0;
            int skippedCount = 0;

            for (Booking booking : bookingsWithoutDeadline) {
                try {
                    // Skip if booking is too old (older than 30 days)
                    if (booking.getCreatedAt() != null &&
                            booking.getCreatedAt().isBefore(LocalDateTime.now().minusDays(30))) {
                        log.debug("Skipping booking ID {} - older than 30 days", booking.getId());
                        skippedCount++;
                        continue;
                    }

                    if (booking.getPnr() == null || booking.getPnr().trim().isEmpty()) {
                        log.warn("Skipping booking ID {} - no PNR", booking.getId());
                        skippedCount++;
                        continue;
                    }

                    if (booking.getChannel() == null || booking.getChannel().trim().isEmpty()) {
                        log.warn("Skipping booking ID {} - no channel", booking.getId());
                        skippedCount++;
                        continue;
                    }

                    log.info("Processing booking ID: {}, Provider: {}, PNR: {}, Channel: {}",
                            booking.getId(), booking.getProviderName(), booking.getPnr(), booking.getChannel());

                    boolean success = ticketingDeadlineService.manualUpdateTicketingDeadline(booking.getId());

                    if (success) {
                        successCount++;
                        log.info("✅ Successfully updated booking ID: {}", booking.getId());
                    } else {
                        failureCount++;
                        log.warn("❌ Failed to update booking ID: {}", booking.getId());
                    }

                    // Small delay between requests to avoid overwhelming the API
                    Thread.sleep(2000);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Scheduler interrupted while processing booking ID: {}", booking.getId());
                    break;
                } catch (Exception e) {
                    failureCount++;
                    log.error("Error processing booking ID {}: {}", booking.getId(), e.getMessage(), e);
                }
            }

            log.info("Ticketing deadline update completed — Total: {}, Success: {}, Failed: {}, Skipped: {}",
                    bookingsWithoutDeadline.size(), successCount, failureCount, skippedCount);

        } catch (Exception e) {
            log.error("Error in scheduled ticketing deadline update job", e);
        }
    }

    private List<Booking> findBookingsWithoutDeadline() {
        List<Booking> combined = new ArrayList<>();
        combined.addAll(bookingRepository.findByProviderName(Provider.SABRE));
        combined.addAll(bookingRepository.findByProviderName(Provider.VERTEIL));
        combined.addAll(bookingRepository.findByProviderName(Provider.GALILEO));


        return combined.stream()
                .filter(b -> b.getStatus() == BookingStatus.PNR)
                .filter(b -> b.getLastPaymentDate() == null || b.getLastPaymentDate().trim().isEmpty())
                .filter(b -> b.getCreatedAt() != null &&
                        b.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .toList();
    }

    /**
     * On-demand manual trigger — can be called from a controller endpoint.
     */
    public String triggerManualUpdate() {
        log.info("Manual trigger of ticketing deadline update requested (Sabre + Verteil)");
        updateMissingTicketingDeadlines();
        return "Ticketing deadline update job triggered manually (Sabre + Verteil). Check logs for details.";
    }

    /**
     * Count of bookings (Sabre + Verteil) that need ticketing deadline update.
     */
    public long getCountOfBookingsNeedingUpdate() {
        return findBookingsWithoutDeadline().size();
    }
}
