package com.aerionsoft.notification.dto.response;

import com.aerionsoft.notification.enums.NotificationChannelType;

public record NotificationPreferenceResponse(
        Long id,
        String typeCode,
        NotificationChannelType channel,
        boolean enabled
) {
}
