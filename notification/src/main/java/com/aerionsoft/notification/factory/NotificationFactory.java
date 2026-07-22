package com.aerionsoft.notification.factory;

import com.aerionsoft.notification.dto.NotificationRequest;
import com.aerionsoft.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationFactory {

    private final NotificationTemplateResolver templateResolver;

    public NotificationFactory(NotificationTemplateResolver templateResolver) {
        this.templateResolver = templateResolver;
    }

    public Notification createFrom(NotificationRequest request) {
        String title = hasText(request.title())
                ? request.title()
                : templateResolver.resolveTitle(request.type(), request.metadata());

        String massage = hasText(request.message())
                ? request.message()
                : templateResolver.resolveBody(request.type(), request.metadata());

        return Notification.create(
                request.recipientUserId(),
                request.type(),
                title,
                massage,
                request.priority()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
