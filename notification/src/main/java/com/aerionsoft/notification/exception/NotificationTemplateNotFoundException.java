package com.aerionsoft.notification.exception;

public class NotificationTemplateNotFoundException extends NotificationException {

    public NotificationTemplateNotFoundException(String message) {
        super(message);
    }

    public static NotificationTemplateNotFoundException forTypeAndLocale(String typeCode, String locale) {
        return new NotificationTemplateNotFoundException(
                "No template found for typeCode=" + typeCode + ", locale=" + locale);
    }

    public static NotificationTemplateNotFoundException forId(Long id) {
        return new NotificationTemplateNotFoundException("No template found with id=" + id);
    }
}
