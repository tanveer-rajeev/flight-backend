package com.aerionsoft.application.event;

import com.aerionsoft.application.service.notification.NotificationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCreatedNotificationListener {

    private final NotificationHelper notificationHelper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingCreated(BookingCreatedNotificationEvent event) {
        notificationHelper.sendBookingConfirmation(
                event.userId(),
                event.userEmail(),
                event.bookingReference(),
                event.pnr(),
                event.currency(),
                event.bookingId(),
                true
        );

        notificationHelper.sendNewBookingNotification(
                1L,
                event.bookingReference(),
                event.userFullName(),
                event.pnr(),
                event.currency()
        );
    }
}
