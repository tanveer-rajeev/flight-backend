package com.aerionsoft.application.event;

import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.service.booking.BookingService;
import com.aerionsoft.application.service.webhook.WebhookAlertDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class HoldToBookFailureListener {

    private final BookingService bookingService;
    private final WebhookAlertDispatchService webhookAlertDispatchService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onHoldToBookFailure(HoldToBookFailureEvent event) {
        try {
            Booking booking = bookingService.getBookingById(event.bookingId());
            if (booking == null) {
                log.warn("Hold-to-book failure event for missing booking id={}", event.bookingId());
                return;
            }
            bookingService.notifyAdminsAboutHoldToBookFailure(booking, event.errorMessage());
            webhookAlertDispatchService.dispatchHoldToBookCoreFailure(booking, null, event.errorMessage());
        } catch (Exception e) {
            log.warn("Failed to process hold-to-book failure notification for booking id={}: {}",
                    event.bookingId(), e.getMessage());
        }
    }
}
