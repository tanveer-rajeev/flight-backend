package com.aerionsoft.application.service.booking;

import com.aerionsoft.application.event.HoldToBookFailureEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HoldToBookFailurePublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publish(Long bookingId, String errorMessage) {
        eventPublisher.publishEvent(new HoldToBookFailureEvent(bookingId, errorMessage));
    }
}
