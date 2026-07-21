package com.aerionsoft.application.service.webhook;

import com.aerionsoft.application.dto.booking.BookingRequest;
import com.aerionsoft.application.dto.booking.core.CoreResponse;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.enums.webhook.WebhookAlertType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebhookAlertDispatchService {

    private final WebhookAlertAsyncDispatcher asyncDispatcher;

    public void dispatchTicketedBookingPostProcessFailure(Booking booking, String ticketNo, String errorMessage) {
        asyncDispatcher.dispatch(
                WebhookAlertType.TICKETED_BOOKING_POST_PROCESS_FAILED,
                WebhookChannelPayloadBuilder.buildTicketedBookingPostProcessFailureMessage(
                        booking, ticketNo, errorMessage));
    }

    public void dispatchBookingCreateCoreFailure(
            BookingRequest request,
            String customerName,
            CoreResponse coreResponse,
            String errorMessage) {
        asyncDispatcher.dispatch(
                WebhookAlertType.BOOKING_CREATE_CORE_FAILED,
                WebhookChannelPayloadBuilder.buildBookingCreateCoreFailureMessage(
                        request, customerName, coreResponse, errorMessage));
    }

    public void dispatchHoldToBookCoreFailure(Booking booking, CoreResponse coreResponse, String errorMessage) {
        asyncDispatcher.dispatch(
                WebhookAlertType.HOLD_TO_BOOK_CORE_FAILED,
                WebhookChannelPayloadBuilder.buildHoldToBookCoreFailureMessage(
                        booking, coreResponse, errorMessage));
    }
}
