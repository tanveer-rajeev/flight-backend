package com.aerionsoft.application.scheduler;

import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.repository.booking.BookingRepository;
import com.aerionsoft.application.service.booking.BookingService;
import com.aerionsoft.application.util.PaymentDeadlineUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Marks USBANGLAAPI, Air Arabia (ARABIA), and Fly Dubai (FLYDUBAI) PNR bookings as
 * {@link BookingStatus#CANCELLED} when the payment time limit has expired.
 * <p>
 * These providers do not call an external cancel API — only the local booking status is updated.
 * Deadline is resolved with {@link PaymentDeadlineUtil} using {@code bookedTimeOffset} / {@code timeOffset}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LccBookingExpiryScheduler {

    private static final boolean ENABLED = false;

    private static final String CANCEL_REASON = "Auto-cancelled: payment time limit expired";

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    // Re-enable: set ENABLED = true and uncomment @Scheduled
    // @Scheduled(cron = "0 0/5 * * * ?")
    public void cancelExpiredLccPnrBookings() {
        if (!ENABLED) {
            return;
        }

        log.info("[LccBookingExpiryScheduler] Checking expired PNR bookings for USBANGLAAPI, ARABIA, FLYDUBAI...");

        try {
            List<Booking> candidates = bookingRepository.findLccPnrBookingsWithPaymentDeadlineForExpiryCancel();

            if (candidates.isEmpty()) {
                log.info("[LccBookingExpiryScheduler] No candidate bookings found.");
                return;
            }

            Instant nowUtc = Instant.now();
            int cancelledCount = 0;
            int skippedCount = 0;

            for (Booking booking : candidates) {
                try {
                    if (booking.getLastPaymentDate() == null || booking.getLastPaymentDate().isBlank()) {
                        skippedCount++;
                        continue;
                    }

                    Instant deadlineUtc;
                    try {
                        deadlineUtc = PaymentDeadlineUtil.resolveDeadlineInstant(booking);
                    } catch (Exception parseEx) {
                        log.warn("[LccBookingExpiryScheduler] Skipping booking ID {} (PNR: {}) — invalid lastPaymentDate '{}': {}",
                                booking.getId(), booking.getPnr(), booking.getLastPaymentDate(), parseEx.getMessage());
                        skippedCount++;
                        continue;
                    }

                    if (nowUtc.isBefore(deadlineUtc)) {
                        skippedCount++;
                        continue;
                    }

                    log.info("[LccBookingExpiryScheduler] Booking ID {} (PNR: {}, Provider: {}) expired. lastPaymentDate: {}, bookedTimeOffset: {}, Deadline UTC: {}, Now UTC: {}. Updating status to CANCELLED.",
                            booking.getId(), booking.getPnr(), booking.getProviderName(),
                            booking.getLastPaymentDate(), booking.getBookedTimeOffset(), deadlineUtc, nowUtc);

                    bookingService.updateBookingStatus(
                            booking.getId(),
                            BookingStatus.CANCELLED,
                            CANCEL_REASON,
                            null,
                            true);

                    cancelledCount++;

                } catch (Exception e) {
                    log.error("[LccBookingExpiryScheduler] Error processing booking ID {}: {}",
                            booking.getId(), e.getMessage(), e);
                }
            }

            log.info("[LccBookingExpiryScheduler] Done. Cancelled: {}, Skipped: {}, Total: {}",
                    cancelledCount, skippedCount, candidates.size());

        } catch (Exception e) {
            log.error("[LccBookingExpiryScheduler] Unexpected error during scheduler run", e);
        }
    }
}
