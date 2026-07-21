package com.aerionsoft.application.dto.notification;

import com.aerionsoft.application.enums.notification.NotificationPriority;
import com.aerionsoft.application.enums.notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplateRequest {

    @NotBlank(message = "Template code is required")
    @Size(max = 100, message = "Template code must be at most 100 characters")
    private String templateCode;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    private NotificationPriority priority;

    @NotBlank(message = "Title template is required")
    @Size(max = 255, message = "Title template must be at most 255 characters")
    private String titleTemplate;

    @NotBlank(message = "Message template is required")
    private String messageTemplate;

    @Size(max = 500, message = "Action URL template must be at most 500 characters")
    private String actionUrlTemplate;

    @Size(max = 100, message = "Action label must be at most 100 characters")
    private String actionLabel;

    private Boolean isActive;
}
