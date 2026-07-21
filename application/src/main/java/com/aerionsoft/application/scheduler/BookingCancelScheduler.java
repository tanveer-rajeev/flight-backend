package com.aerionsoft.application.scheduler;

import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.enums.notification.NotificationPriority;
import com.aerionsoft.application.enums.notification.NotificationType;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.booking.BookingRepository;
import com.aerionsoft.application.service.booking.BookingCoordinatorService;
import com.aerionsoft.application.service.notification.NotificationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler that auto-cancels Sabre and Galileo PNR bookings before the payment deadline.
 * <p>
 * Logic:
 * - deadline = {@code lastPaymentDate} as UTC ISO-8601 instant (e.g. {@code 2026-06-03T17:59:00Z})
 * - If {@code nowUtc >= deadline - 25 min} AND {@code nowUtc < deadline} → cancel the booking
 * - If deadline already passed → cancel once (late catch-up)
 * - Failed cancels are retried up to 3 times per booking ({@code auto_cancel_failure_count}); then skipped
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCancelScheduler {

    private static final long CANCEL_LEAD_SECONDS = 10 * 60L;
    private static final int MAX_CANCEL_ATTEMPTS = 3;
    private static final long RETRY_DELAY_SECONDS = 5L;

    private final BookingRepository bookingRepository;
    private final BookingCoordinatorService bookingCoordinatorService;
    private final NotificationHelper notificationHelper;
    private final AdminUserRepository adminUserRepository;

    /**
     * Runs every 5 minutes.
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void cancelExpiringBookings() {
        log.info("[BookingCancelScheduler] Starting check for expiring Sabre/Galileo PNR bookings...");

        try {
            List<Booking> candidates = bookingRepository.findPnrBookingsWithPaymentDeadlineForAutoCancel();

            if (candidates.isEmpty()) {
                log.info("[BookingCancelScheduler] No PNR bookings with payment deadline found.");
                return;
            }

            log.info("[BookingCancelScheduler] Found {} candidate bookings. Checking deadlines...", candidates.size());

            Instant nowUtc = Instant.now();
            int cancelledCount = 0;
            int skippedCount = 0;

            for (Booking booking : candidates) {
                try {
                    if (booking.getLastPaymentDate() == null || booking.getLastPaymentDate().isBlank()) {
                        skippedCount++;
                        continue;
                    }

                    if (booking.getPnr() == null || booking.getPnr().isBlank()) {
                        log.warn("[BookingCancelScheduler] Skipping booking ID {} — no PNR", booking.getId());
                        skippedCount++;
                        continue;
                    }

                    if (booking.getChannel() == null || booking.getChannel().isBlank()) {
                        log.warn("[BookingCancelScheduler] Skipping booking ID {} — no channel", booking.getId());
                        skippedCount++;
                        continue;
                    }

                    if (hasExceededCancelAttempts(booking)) {
                        log.warn("[BookingCancelScheduler] Skipping booking ID {} (PNR: {}) — auto-cancel failed {} times",
                                booking.getId(), booking.getPnr(), MAX_CANCEL_ATTEMPTS);
                        skippedCount++;
                        continue;
                    }

                    Instant deadlineUtc = parseLastPaymentDateUtc(booking.getLastPaymentDate());
                    Instant windowStart = deadlineUtc.minusSeconds(CANCEL_LEAD_SECONDS);

                    if (!nowUtc.isBefore(windowStart) && nowUtc.isBefore(deadlineUtc)) {
                        log.info("[BookingCancelScheduler] Booking ID {} (PNR: {}, Provider: {}) within 25-min cancellation window. Deadline UTC: {}, Now UTC: {}",
                                booking.getId(), booking.getPnr(), booking.getProviderName(), deadlineUtc, nowUtc);
                        if (cancelBookingAndNotify(booking, deadlineUtc)) {
                            cancelledCount++;
                        } else {
                            skippedCount++;
                        }
                    } else if (!nowUtc.isBefore(deadlineUtc)) {
                        log.warn("[BookingCancelScheduler] Booking ID {} (PNR: {}) deadline already passed (deadline UTC: {}, now UTC: {}). Cancelling.",
                                booking.getId(), booking.getPnr(), deadlineUtc, nowUtc);
                        if (cancelBookingAndNotify(booking, deadlineUtc)) {
                            cancelledCount++;
                        } else {
                            skippedCount++;
                        }
                    } else {
                        long minutesLeft = (deadlineUtc.getEpochSecond() - nowUtc.getEpochSecond()) / 60;
                        log.debug("[BookingCancelScheduler] Booking ID {} (PNR: {}) has {} minutes until deadline. Skipping.",
                                booking.getId(), booking.getPnr(), minutesLeft);
                        skippedCount++;
                    }

                } catch (Exception e) {
                    log.error("[BookingCancelScheduler] Error processing booking ID {}: {}",
                            booking.getId(), e.getMessage(), e);
                }
            }

            log.info("[BookingCancelScheduler] Done. Cancelled: {}, Skipped: {}, Total: {}",
                    cancelledCount, skippedCount, candidates.size());

        } catch (Exception e) {
            log.error("[BookingCancelScheduler] Unexpected error during scheduler run", e);
        }
    }

    private Instant parseLastPaymentDateUtc(String lastPaymentDate) {
        return Instant.parse(lastPaymentDate.trim());
    }

    private boolean hasExceededCancelAttempts(Booking booking) {
        return booking.getAutoCancelFailureCount() >= MAX_CANCEL_ATTEMPTS;
    }

    private int getAutoCancelFailureCount(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .map(Booking::getAutoCancelFailureCount)
                .orElse(MAX_CANCEL_ATTEMPTS);
    }

    private void recordCancelFailure(Long bookingId) {
        bookingRepository.incrementAutoCancelFailureCount(bookingId);
    }

    private void clearCancelFailures(Long bookingId) {
        bookingRepository.resetAutoCancelFailureCount(bookingId);
    }

    /**
     * @return true if cancel succeeded, false if all attempts failed or booking is blocked
     */
    private boolean cancelBookingAndNotify(Booking booking, Instant deadlineUtc) {
        Long bookingId = booking.getId();
        String pnr = booking.getPnr();
        String channel = booking.getChannel();
        String reason = "Auto-cancelled: payment deadline approaching (scheduled cancellation)";

        if (hasExceededCancelAttempts(booking)) {
            log.warn("[BookingCancelScheduler] Skipping cancel for booking ID {} (PNR: {}) — already failed {} times",
                    bookingId, pnr, MAX_CANCEL_ATTEMPTS);
            return false;
        }

        int failuresSoFar = booking.getAutoCancelFailureCount();
        int attemptsRemaining = MAX_CANCEL_ATTEMPTS - failuresSoFar;

        for (int attempt = 1; attempt <= attemptsRemaining; attempt++) {
            int attemptNumber = failuresSoFar + attempt;
            try {
                log.info("[BookingCancelScheduler] Cancel attempt {}/{} for booking ID {}, PNR: {}, Channel: {}, Provider: {}",
                        attemptNumber, MAX_CANCEL_ATTEMPTS, bookingId, pnr, channel, booking.getProviderName());

                bookingCoordinatorService.cancelByPnr(pnr, channel, reason);

                clearCancelFailures(bookingId);
                log.info("[BookingCancelScheduler] Successfully cancelled booking ID {} (PNR: {})",
                        bookingId, pnr);
                notifyAdmins(booking, deadlineUtc);
                return true;

            } catch (Exception e) {
                recordCancelFailure(bookingId);
                int totalFailures = getAutoCancelFailureCount(bookingId);
                log.error("[BookingCancelScheduler] Cancel attempt {}/{} failed for booking ID {} (PNR: {}): {}",
                        attemptNumber, MAX_CANCEL_ATTEMPTS, bookingId, pnr, e.getMessage(), e);

                if (totalFailures >= MAX_CANCEL_ATTEMPTS) {
                    log.error("[BookingCancelScheduler] Giving up on booking ID {} (PNR: {}) after {} failed attempts",
                            bookingId, pnr, MAX_CANCEL_ATTEMPTS);
                    return false;
                }

                if (attempt < attemptsRemaining) {
                    try {
                        TimeUnit.SECONDS.sleep(RETRY_DELAY_SECONDS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }

        return false;
    }

    private void notifyAdmins(Booking booking, Instant deadlineUtc) {
        try {
            List<AdminUser> admins = adminUserRepository.findAdminsByRoleSlug("admin");

            if (admins == null || admins.isEmpty()) {
                log.warn("[BookingCancelScheduler] No admin users found to notify for booking ID {}", booking.getId());
                return;
            }

            String bookingRef = booking.getBookingReference() != null ? booking.getBookingReference() : "N/A";
            String pnr = booking.getPnr() != null ? booking.getPnr() : "N/A";
            String provider = booking.getProviderName() != null ? booking.getProviderName().name() : "N/A";
            String userName = booking.getCreatedBy() != null ?
                    (booking.getCreatedBy().getFullName() != null ?
                            booking.getCreatedBy().getFullName() :
                            booking.getCreatedBy().getEmail()) :
                    "Unknown User";
            String currency = booking.getExchangeCurrency() != null ? booking.getExchangeCurrency() : "USD";
            String price = booking.getBookingPrice() != null ? booking.getBookingPrice() : "N/A";

            LocalDateTime deadlineDateTime = LocalDateTime.ofInstant(deadlineUtc, ZoneOffset.UTC);

            String title = "Booking Auto-Cancelled";
            String message = String.format(
                    "Booking auto-cancelled due to expiring payment deadline. " +
                    "Provider: %s | Agent: %s | Ref: %s | PNR: %s | Amount: %s %s | Deadline (UTC): %s",
                    provider, userName, bookingRef, pnr, price, currency, deadlineDateTime);
            String actionUrl = "/bookings/" + booking.getId();

            for (AdminUser admin : admins) {
                if (admin != null && admin.getId() != null) {
                    try {
                        notificationHelper.sendCustomNotification(
                                admin.getId(),
                                NotificationType.BOOKING_CANCELLED,
                                NotificationPriority.HIGH,
                                title,
                                message,
                                actionUrl,
                                "View Booking"
                        );
                    } catch (Exception e) {
                        log.error("[BookingCancelScheduler] Failed to notify admin ID {}: {}",
                                admin.getId(), e.getMessage());
                    }
                }
            }

            log.info("[BookingCancelScheduler] Notified {} admin(s) about cancellation of booking ID {}",
                    admins.size(), booking.getId());

        } catch (Exception e) {
            log.error("[BookingCancelScheduler] Error sending admin notifications for booking ID {}: {}",
                    booking.getId(), e.getMessage(), e);
        }
    }
}
