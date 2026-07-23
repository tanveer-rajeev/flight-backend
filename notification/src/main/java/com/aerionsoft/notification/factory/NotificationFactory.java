package com.aerionsoft.notification.factory;

import com.aerionsoft.notification.dto.request.NotificationRequest;
import com.aerionsoft.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationFactory {

    private final NotificationTemplateResolver templateResolver;

    public NotificationFactory(NotificationTemplateResolver templateResolver) {
        this.templateResolver = templateResolver;
    }

    public Notification createFrom(NotificationRequest request) {
        ResolvedNotificationContent resolved = templateResolver.resolve(request.type(), request.metadata());

        String title = hasText(request.title()) ? request.title() : resolved.title();
        String message = hasText(request.message()) ? request.message() : resolved.message();
        var priority = (request.priority() != null) ? request.priority() : resolved.defaultPriority();
        String referenceType = hasText(request.referenceType()) ? request.referenceType() : resolved.defaultReferenceType();

        Notification notification = Notification.create(
                request.userId(),
                request.type(),
                title,
                message,
                priority
        );

        notification.setReferenceType(referenceType);
        notification.setReferenceId(request.referenceId());
        notification.setActionUrl(request.actionUrl());
        notification.setActionLabel(request.actionLabel());

        return notification;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}