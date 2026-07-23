package com.aerionsoft.notification.dto.response;

import com.aerionsoft.notification.enums.NotificationPriority;

import java.time.LocalDateTime;

public record NotificationTemplateResponse(
        Long id,
        String typeCode,
        String locale,
        String titleTemplate,
        String messageTemplate,
        NotificationPriority defaultPriority,
        String defaultReferenceType,
        LocalDateTime updatedAt
) {
}