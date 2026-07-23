package com.aerionsoft.notification.factory;

import com.aerionsoft.notification.entity.NotificationTemplate;
import com.aerionsoft.notification.entity.NotificationType;
import com.aerionsoft.notification.enums.NotificationPriority;
import com.aerionsoft.notification.repository.NotificationTemplateRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationTemplateResolver {

    private static final String DEFAULT_LOCALE = "en";

    private final NotificationTemplateRepository templateRepository;

    public NotificationTemplateResolver(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public ResolvedNotificationContent resolve(NotificationType type, Map<String, Object> metadata) {
        return resolve(type, metadata, DEFAULT_LOCALE);
    }

    public ResolvedNotificationContent resolve(NotificationType type, Map<String, Object> metadata, String locale) {
        NotificationTemplate template = fetchTemplate(type.getCode(), locale);

        if (template == null) {
            return new ResolvedNotificationContent(type.getCode(), "", NotificationPriority.MEDIUM, null);
        }

        return new ResolvedNotificationContent(
                PlaceholderInterpolator.interpolate(template.getTitleTemplate(), metadata),
                PlaceholderInterpolator.interpolate(template.getMessageTemplate(), metadata),
                template.getDefaultPriority(),
                template.getDefaultReferenceType()
        );
    }

    @Cacheable(value = "notificationTemplates", key = "#typeCode + '-' + #locale")
    protected NotificationTemplate fetchTemplate(String typeCode, String locale) {
        return templateRepository.findByTypeCodeAndLocale(typeCode, locale)
                .or(() -> templateRepository.findByTypeCodeAndLocale(typeCode, DEFAULT_LOCALE))
                .orElse(null);
    }
}