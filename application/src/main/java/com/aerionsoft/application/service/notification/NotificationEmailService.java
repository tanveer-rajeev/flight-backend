package com.aerionsoft.application.service.notification;

import com.aerionsoft.application.entity.NotificationDeliveryLog;
import com.aerionsoft.application.enums.notification.DeliveryChannel;
import com.aerionsoft.application.enums.notification.DeliveryStatus;
import com.aerionsoft.application.repository.notification.NotificationDeliveryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEmailService {

    private final JavaMailSender mailSender;
    private final NotificationDeliveryLogRepository deliveryLogRepository;
    private final NotificationPreferenceService preferenceService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send email notification asynchronously
     * This method runs in a separate thread and doesn't block the main request
     */
    @Async
    public void sendEmailNotification(Long notificationId, Long userId, String userEmail,
                                      String subject, String message, String actionUrl) {

        log.debug("Async email sending started for notification: {} to user: {}", notificationId, userId);

        // Check if user has email notifications enabled
        if (!preferenceService.isChannelEnabled(userId, "EMAIL")) {
            log.info("Email notifications disabled for user: {}", userId);
            return;
        }

        // Check quiet hours
        if (preferenceService.isInQuietHours(userId)) {
            log.info("User {} is in quiet hours, skipping email notification", userId);
            return;
        }

        // Create delivery log
        NotificationDeliveryLog deliveryLog = NotificationDeliveryLog.builder()
                .notificationId(notificationId)
                .deliveryChannel(DeliveryChannel.EMAIL)
                .status(DeliveryStatus.PENDING)
                .recipient(userEmail)
                .build();
        deliveryLog = deliveryLogRepository.save(deliveryLog);

        try {
            sendHtmlEmail(userEmail, subject, message, actionUrl);

            deliveryLog.markAsSent();
            deliveryLog.markAsDelivered();
            deliveryLogRepository.save(deliveryLog);

            log.info("Email notification sent successfully to: {}", userEmail);
        } catch (Exception e) {
            deliveryLog.markAsFailed(e.getMessage());
            deliveryLogRepository.save(deliveryLog);

            log.error("Failed to send email notification to: {}", userEmail, e);
        }
    }

    /**
     * Send HTML email with action button
     */
    private void sendHtmlEmail(String to, String subject, String message, String actionUrl) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);

        String htmlContent = buildHtmlEmailContent(subject, message, actionUrl);
        helper.setText(htmlContent, true);

        mailSender.send(mimeMessage);
    }

    /**
     * Send simple text email asynchronously
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String message) {
        try {
            log.debug("Async simple email sending started for: {}", to);

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(to);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);

            mailSender.send(mailMessage);
            log.info("Simple email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send simple email to: {}", to, e);
        }
    }

    /**
     * Build HTML email template
     */
    private String buildHtmlEmailContent(String title, String message, String actionUrl) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append(".header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }");
        html.append(".content { background-color: #f9f9f9; padding: 20px; margin: 20px 0; }");
        html.append(".button { display: inline-block; padding: 12px 24px; background-color: #4CAF50; ");
        html.append("color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }");
        html.append(".footer { text-align: center; color: #777; font-size: 12px; margin-top: 20px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h1>").append(escapeHtml(title)).append("</h1>");
        html.append("</div>");
        html.append("<div class='content'>");
        html.append("<p>").append(escapeHtml(message).replace("\n", "<br>")).append("</p>");

        if (actionUrl != null && !actionUrl.isEmpty()) {
            html.append("<div style='text-align: center;'>");
            html.append("<a href='").append(escapeHtml(actionUrl)).append("' class='button'>View Details</a>");
            html.append("</div>");
        }

        html.append("</div>");
        html.append("<div class='footer'>");
        html.append("<p>This is an automated notification. Please do not reply to this email.</p>");
        html.append("<p>&copy; 2025 Flight Booking System. All rights reserved.</p>");
        html.append("</div>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}

