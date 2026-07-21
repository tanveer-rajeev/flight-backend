package com.aerionsoft.application.service.notification;

import com.aerionsoft.application.entity.NotificationTemplate;
import com.aerionsoft.application.enums.notification.NotificationPriority;
import com.aerionsoft.application.enums.notification.NotificationType;
import com.aerionsoft.application.repository.notification.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateInitializer implements CommandLineRunner {

    private final NotificationTemplateRepository templateRepository;
    @Value("${app.domain:http://localhost:8080}")
    private String appDomain;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing notification templates...");
        initializeTemplates();
        log.info("Notification templates initialization completed.");
    }

    private void initializeTemplates() {
        log.info("Creating missing default notification templates...");
        List<NotificationTemplate> templates = Arrays.asList(
                // Booking Related
                createTemplate(
                        "BOOKING_CONFIRMED",
                        NotificationType.BOOKING_CONFIRMED,
                        NotificationPriority.HIGH,
                        "Booking Confirmed: {{bookingReference}}",
                        "Your booking {{bookingReference}} has been confirmed. Pnr: {{pnr}} .",
                        appDomain + "/dashboard/bookings/{{bookingId}}",
                        "View Booking"
                ),
                createTemplate(
                        "BOOKING_CANCELLED",
                        NotificationType.BOOKING_CANCELLED,
                        NotificationPriority.HIGH,
                        "Booking Cancelled: {{bookingReference}}",
                        "Your booking {{bookingReference}} has been cancelled.",
                        "/bookings/{{bookingId}}",
                        "View Booking"
                ),
                createTemplate(
                        "TICKET_ISSUED",
                        NotificationType.TICKET_ISSUED,
                        NotificationPriority.HIGH,
                        "Ticket Issued: {{pnr}}",
                        "Your ticket for booking {{bookingReference}} has been issued. PNR: {{pnr}}",
                        "/dashboard/bookings/{{bookingId}}",
                        "Download Ticket"
                ),
                createTemplate(
                        "PRICE_CHANGED",
                        NotificationType.PRICE_CHANGED,
                        NotificationPriority.HIGH,
                        "Price Change Alert",
                        "The price for booking {{bookingReference}} has changed from {{oldPrice}} to {{newPrice}} {{currency}}.",
                        "/bookings/{{bookingId}}",
                        "Review Booking"
                ),

                // Payment Related
                createTemplate(
                        "PAYMENT_SUCCESS",
                        NotificationType.PAYMENT_SUCCESS,
                        NotificationPriority.HIGH,
                        "Payment Successful",
                        "We have received your payment of {{amount}} {{currency}}.",
                        "/transaction-history",
                        "View Wallet"
                ),
                createTemplate(
                        "PAYMENT_FAILED",
                        NotificationType.PAYMENT_FAILED,
                        NotificationPriority.HIGH,
                        "Payment Failed",
                        "Your payment of {{amount}} {{currency}} failed. Reason: {{reason}}",
                        "/dashboard/transaction-history",
                        "Retry Payment"
                ),

                // Application Updates
                createTemplate(
                        "VISA_APPLICATION_UPDATE",
                        NotificationType.VISA_APPLICATION_UPDATE,
                        NotificationPriority.MEDIUM,
                        "Visa Application Update",
                        "Your visa application for {{country}} status has been updated to {{status}}.",
                        "/visa/applications/{{applicationId}}",
                        "View Application"
                ),
                createTemplate(
                        "TOUR_APPLICATION_UPDATE",
                        NotificationType.TOUR_APPLICATION_UPDATE,
                        NotificationPriority.MEDIUM,
                        "Tour Application Update",
                        "Your tour application for {{package}} status has been updated to {{status}}.",
                        "/tour/applications/{{applicationId}}",
                        "View Application"
                ),

                // Account & System
                createTemplate(
                        "ACCOUNT_UPDATE",
                        NotificationType.ACCOUNT_UPDATE,
                        NotificationPriority.MEDIUM,
                        "Account Update",
                        "{{message}}",
                        "/profile",
                        "View Profile"
                ),
                createTemplate(
                        "SYSTEM_ALERT",
                        NotificationType.SYSTEM_ALERT,
                        NotificationPriority.HIGH,
                        "System Alert",
                        "{{message}}",
                        null,
                        null
                ),
                createTemplate(
                        "PROMOTION",
                        NotificationType.PROMOTION,
                        NotificationPriority.LOW,
                        "{{title}}",
                        "{{message}}",
                        "{{url}}",
                        "View Offer"
                ),
                createTemplate(
                        "GENERAL",
                        NotificationType.GENERAL,
                        NotificationPriority.MEDIUM,
                        "{{title}}",
                        "{{message}}",
                        null,
                        null
                ),
                createTemplate(
                        "WELCOME_USER",
                        NotificationType.WELCOME_USER,
                        NotificationPriority.MEDIUM,
                        "Welcome to KingStarTravel!",
                        "Hello {{userName}}, welcome to KingStarTravel! We are excited to have you on board.",
                        appDomain + "/dashboard",
                        "Get Started"
                ),
                createTemplate(
                        "ACCOUNT_VERIFIED",
                        NotificationType.ACCOUNT_VERIFIED,
                        NotificationPriority.MEDIUM,
                        "Account Verified Successfully",
                        "Congratulations {{userName}}! Your account has been verified successfully. You can now enjoy all features of our platform.",
                        "/profile",
                        "View Profile"
                ),
                createTemplate(
                        "NEW_USER_REGISTERED",
                        NotificationType.GENERAL,
                        NotificationPriority.LOW,
                        "New User Registration",
                        "A new user {{userName}} ({{userEmail}}) has registered on the platform.",
                        "/admin/users",
                        "View Users"
                ),
                createTemplate(
                        "NEW_BOOKING_CREATED",
                        NotificationType.BOOKING_CONFIRMED,
                        NotificationPriority.MEDIUM,
                        "New Booking Created - {{bookingReference}}",
                        "A new booking {{bookingReference}} has been created by {{userName}} for {{amount}} {{currency}}.",
                        "/admin/bookings/{{bookingReference}}",
                        "View Booking"
                ),
                createTemplate(
                        "LOGIN_ALERT",
                        NotificationType.LOGIN_ALERT,
                        NotificationPriority.MEDIUM,
                        "New Login Detected",
                        "Hello {{userName}}, a new login was detected on your account at {{loginTime}}. Location: {{ipAddress}}. Device: {{userAgent}}. If this wasn't you, please secure your account immediately.",
                        appDomain + "/dashboard",
                        "View Security"
                )
        );

        int createdCount = 0;
        for (NotificationTemplate template : templates) {
            if (!templateRepository.existsByTemplateCode(template.getTemplateCode())) {
                templateRepository.save(template);
                createdCount++;
                log.info("Created missing notification template: {}", template.getTemplateCode());
            }
        }

        log.info("Default notification template check completed. Created {} missing templates.", createdCount);
    }

    private NotificationTemplate createTemplate(String code, NotificationType type, NotificationPriority priority,
                                                String title, String message, String url, String label) {
        return NotificationTemplate.builder()
                .templateCode(code)
                .type(type)
                .priority(priority)
                .titleTemplate(title)
                .messageTemplate(message)
                .actionUrlTemplate(url)
                .actionLabel(label)
                .isActive(true)
                .build();
    }
}
