package com.aerionsoft.notification.facade;

import com.aerionsoft.notification.dto.request.NotificationRequest;
import com.aerionsoft.notification.dto.response.NotificationResponse;
import com.aerionsoft.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationFacade {
    private final NotificationService service;

    public NotificationResponse send(NotificationRequest request) {
        return service.send(request);
    }
}
