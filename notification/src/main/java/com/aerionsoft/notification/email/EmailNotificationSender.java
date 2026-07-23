package com.aerionsoft.notification.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationSender {

    private final EmailClient emailClient;

    public EmailNotificationSender(EmailClient emailClient) {
        this.emailClient = emailClient;
    }

    public void send(String recipientEmail, String subject, String message) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email is missing — cannot send notification email");
        }
        log.debug("Sending email notification to {}", recipientEmail);
        emailClient.sendEmail(recipientEmail, subject, message);
    }
}
