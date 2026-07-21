package com.aerionsoft.application.service.notification;

import com.aerionsoft.application.dto.DailyReportResponseDTO;
import com.aerionsoft.application.dto.notification.CreateNotificationFromTemplateRequest;
import com.aerionsoft.application.dto.notification.CreateNotificationRequest;
import com.aerionsoft.application.enums.notification.NotificationPriority;
import com.aerionsoft.application.enums.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper service for creating common notifications throughout the application
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationHelper {

    private final NotificationService notificationService;

    /**
     * Send booking confirmation notification
     */
    public void sendBookingConfirmation(Long userId, String bookingReference,
                                       String amount, String currency, Long bookingId) {
        Map<String, String> variables = new HashMap<>();
        variables.put("bookingReference", bookingReference);
        variables.put("amount", amount);
        variables.put("currency", currency);
        variables.put("bookingId", bookingId.toString());

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("BOOKING_CONFIRMED")
                .userId(userId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request);
            log.info("Booking confirmation notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send booking confirmation notification", e);
        }
    }

    /**
     * Send booking confirmation notification with email
     */
    public void sendBookingConfirmation(Long userId, String userEmail, String bookingReference,
                                       String amount, String currency, Long bookingId, boolean sendEmail) {
        Map<String, String> variables = new HashMap<>();
        variables.put("bookingReference", bookingReference);
        variables.put("pnr", amount);
//        variables.put("currency", currency);
        variables.put("bookingId", bookingId.toString());

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("BOOKING_CONFIRMED")
                .userId(userId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request, userEmail, sendEmail);
            log.info("Booking confirmation notification sent to user: {} with email: {}", userId, sendEmail);
        } catch (Exception e) {
            log.error("Failed to send booking confirmation notification", e);
        }
    }

    /**
     * Send payment success notification
     */
    public void sendPaymentSuccess(Long userId, String amount, String currency, String transactionId) {
        Map<String, String> variables = new HashMap<>();
        variables.put("amount", amount);
        variables.put("currency", currency);
        variables.put("transactionId", transactionId);

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("PAYMENT_SUCCESS")
                .userId(userId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request);
            log.info("Payment success notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send payment success notification", e);
        }
    }

    /**
     * Send payment success notification with email
     */
    public void sendPaymentSuccess(Long userId, String userEmail, String amount, String currency,
                                   String transactionId, boolean sendEmail) {
        Map<String, String> variables = new HashMap<>();
        variables.put("amount", amount);
        variables.put("currency", currency);
        variables.put("transactionId", transactionId);

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("PAYMENT_SUCCESS")
                .userId(userId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request, userEmail, sendEmail);
            log.info("Payment success notification sent to user: {} with email: {}", userId, sendEmail);
        } catch (Exception e) {
            log.error("Failed to send payment success notification", e);
        }
    }

    /**
     * Send payment failed notification
     */
    public void sendPaymentFailed(Long userId, String amount, String currency,
                                 String reason, String paymentId) {
        Map<String, String> variables = new HashMap<>();
        variables.put("amount", amount);
        variables.put("currency", currency);
        variables.put("reason", reason);
        variables.put("paymentId", paymentId);

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("PAYMENT_FAILED")
                .userId(userId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request);
            log.info("Payment failed notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send payment failed notification", e);
        }
    }

    /**
     * Send payment failed notification with email
     */
    public void sendPaymentFailed(Long userId, String userEmail, String amount, String currency,
                                 String reason, String paymentId, boolean sendEmail) {
        Map<String, String> variables = new HashMap<>();
        variables.put("amount", amount);
        variables.put("currency", currency);
        variables.put("reason", reason);
        variables.put("paymentId", paymentId);

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("PAYMENT_FAILED")
                .userId(userId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request, userEmail, sendEmail);
            log.info("Payment failed notification sent to user: {} with email: {}", userId, sendEmail);
        } catch (Exception e) {
            log.error("Failed to send payment failed notification", e);
        }
    }

    /**
     * Send ticket issued notification
     */
    public void sendTicketIssued(Long userId, String bookingReference, String pnr, Long bookingId) {
        Map<String, String> variables = new HashMap<>();
        variables.put("bookingReference", bookingReference);
        variables.put("pnr", pnr);
        variables.put("bookingId", bookingId.toString());

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("TICKET_ISSUED")
                .userId(userId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request);
            log.info("Ticket issued notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send ticket issued notification", e);
        }
    }

    /**
     * Send ticket issued notification with email
     */
    public void sendTicketIssued(Long userId, String userEmail, String bookingReference,
                                String pnr, Long bookingId, boolean sendEmail) {
        Map<String, String> variables = new HashMap<>();
        variables.put("bookingReference", bookingReference);
        variables.put("pnr", pnr);
        variables.put("bookingId", bookingId.toString());

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("TICKET_ISSUED")
                .userId(userId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request, userEmail, sendEmail);
            log.info("Ticket issued notification sent to user: {} with email: {}", userId, sendEmail);
        } catch (Exception e) {
            log.error("Failed to send ticket issued notification", e);
        }
    }

    /**
     * Send price change alert
     */
    public void sendPriceChangeAlert(Long userId, String bookingReference,
                                    String oldPrice, String newPrice,
                                    String currency, Long bookingId) {
        Map<String, String> variables = new HashMap<>();
        variables.put("bookingReference", bookingReference);
        variables.put("oldPrice", oldPrice);
        variables.put("newPrice", newPrice);
        variables.put("currency", currency);
        variables.put("bookingId", bookingId.toString());

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("PRICE_CHANGED")
                .userId(userId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request);
            log.info("Price change alert sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send price change alert", e);
        }
    }

    /**
     * Send price change alert with email
     */
    public void sendPriceChangeAlert(Long userId, String userEmail, String bookingReference,
                                    String oldPrice, String newPrice, String currency,
                                    Long bookingId, boolean sendEmail) {
        Map<String, String> variables = new HashMap<>();
        variables.put("bookingReference", bookingReference);
        variables.put("oldPrice", oldPrice);
        variables.put("newPrice", newPrice);
        variables.put("currency", currency);
        variables.put("bookingId", bookingId.toString());

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("PRICE_CHANGED")
                .userId(userId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request, userEmail, sendEmail);
            log.info("Price change alert sent to user: {} with email: {}", userId, sendEmail);
        } catch (Exception e) {
            log.error("Failed to send price change alert", e);
        }
    }

    /**
     * Send booking cancellation notification
     */
    public void sendBookingCancellation(Long userId, String bookingReference, Long bookingId) {
        Map<String, String> variables = new HashMap<>();
        variables.put("bookingReference", bookingReference);
        variables.put("bookingId", bookingId.toString());

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("BOOKING_CANCELLED")
                .userId(userId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request);
            log.info("Booking cancellation notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send booking cancellation notification", e);
        }
    }

    /**
     * Send booking cancellation with email
     */
    public void sendBookingCancellation(Long userId, String userEmail, String bookingReference,
                                       Long bookingId, boolean sendEmail) {
        Map<String, String> variables = new HashMap<>();
        variables.put("bookingReference", bookingReference);
        variables.put("bookingId", bookingId.toString());

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("BOOKING_CANCELLED")
                .userId(userId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request, userEmail, sendEmail);
            log.info("Booking cancellation notification sent to user: {} with email: {}", userId, sendEmail);
        } catch (Exception e) {
            log.error("Failed to send booking cancellation notification", e);
        }
    }

    /**
     * Send system alert
     */
    public void sendSystemAlert(Long userId, String message, NotificationPriority priority) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId(userId)
                .type(NotificationType.SYSTEM_ALERT)
                .priority(priority != null ? priority : NotificationPriority.HIGH)
                .title("System Alert")
                .message(message)
                .build();

        try {
            notificationService.createNotification(request);
            log.info("System alert sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send system alert", e);
        }
    }

    /**
     * Send custom notification
     */
    public void sendCustomNotification(Long userId, NotificationType type,
                                      NotificationPriority priority,
                                      String title, String message,
                                      String actionUrl, String actionLabel) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId(userId)
                .type(type)
                .priority(priority)
                .title(title)
                .message(message)
                .actionUrl(actionUrl)
                .actionLabel(actionLabel)
                .build();

        try {
            notificationService.createNotification(request);
            log.info("Custom notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send custom notification", e);
        }
    }

    /**
     * Send new user registration notification to admin
     */
    public void sendNewUserRegistrationNotification(Long adminId, String userName, String userEmail) {
        Map<String, String> variables = new HashMap<>();
        variables.put("userName", userName);
        variables.put("userEmail", userEmail);

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("NEW_USER_REGISTERED")
                .userId(adminId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request);
            log.info("New user registration notification sent to admin: {}", adminId);
        } catch (Exception e) {
            log.error("Failed to send new user registration notification", e);
        }
    }

    /**
     * Send new user admin notification
     */
    public void sendNewUserAdminNotification(Long adminId, String userName, String userEmail) {
        sendNewUserRegistrationNotification(adminId, userName, userEmail);
    }

    /**
     * Send new booking notification to admin
     */
    public void sendNewBookingNotification(Long adminId, String bookingReference, String userName, String amount, String currency) {
        String title = "New Booking Created";
        String message = String.format("New booking created by %s. Ref: %s, Amount: %s %s",
                userName, bookingReference, amount, currency);

        try {
            sendCustomNotification(
                    adminId,
                    NotificationType.BOOKING_CONFIRMED,
                    NotificationPriority.HIGH,
                    title,
                    message,
                    "/admin/bookings",
                    "View Booking"
            );
            log.info("New booking notification sent to admin: {}", adminId);
        } catch (Exception e) {
            log.error("Failed to send new booking notification", e);
        }
    }

    /**
     * Send new booking admin notification
     */
    public void sendNewBookingAdminNotification(Long adminId, String bookingReference, String amount, String currency) {
        String title = "New Booking Created";
        String message = String.format("New booking created. Ref: %s, Amount: %s %s",
                bookingReference, amount, currency);

        try {
            sendCustomNotification(
                    adminId,
                    NotificationType.BOOKING_CONFIRMED,
                    NotificationPriority.HIGH,
                    title,
                    message,
                    "/admin/bookings",
                    "View Booking"
            );
            log.info("New booking admin notification sent to admin: {}", adminId);
        } catch (Exception e) {
            log.error("Failed to send new booking admin notification", e);
        }
    }

    /**
     * Send welcome notification to new user
     */
    public void sendWelcomeNotification(Long userId, String userName) {
        Map<String, String> variables = new HashMap<>();
        variables.put("userName", userName);

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("WELCOME_USER")
                .userId(userId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request);
            log.info("Welcome notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send welcome notification", e);
        }
    }

    /**
     * Send welcome notification to new user with email
     */
    public void sendWelcomeNotification(Long userId, String userEmail, String userName, boolean sendEmail) {
        Map<String, String> variables = new HashMap<>();
        variables.put("userName", userName);

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("WELCOME_USER")
                .userId(userId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request, userEmail, sendEmail);
            log.info("Welcome notification sent to user: {} with email: {}", userId, sendEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome notification", e);
        }
    }

    /**
     * Send login alert notification
     */
    public void sendLoginAlert(Long userId, String userName, String loginTime,
                              String ipAddress, String userAgent) {
        Map<String, String> variables = new HashMap<>();
        variables.put("userName", userName);
        variables.put("loginTime", loginTime);
        variables.put("ipAddress", ipAddress);
        variables.put("userAgent", userAgent);

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("LOGIN_ALERT")
                .userId(userId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request);
            log.info("Login alert notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send login alert notification", e);
        }
    }

    /**
     * Send login alert notification with email
     */
    public void sendLoginAlert(Long userId, String userEmail, String userName,
                              String loginTime, String ipAddress, String userAgent, boolean sendEmail) {
        Map<String, String> variables = new HashMap<>();
        variables.put("userName", userName);
        variables.put("loginTime", loginTime);
        variables.put("ipAddress", ipAddress);
        variables.put("userAgent", userAgent);

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("LOGIN_ALERT")
                .userId(userId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request, userEmail, sendEmail);
            log.info("Login alert notification sent to user: {} with email: {}", userId, sendEmail);
        } catch (Exception e) {
            log.error("Failed to send login alert notification", e);
        }
    }

    /**
     * Send account verified notification
     */
    public void sendAccountVerifiedNotification(Long userId, String userName) {
        Map<String, String> variables = new HashMap<>();
        variables.put("userName", userName);

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("ACCOUNT_VERIFIED")
                .userId(userId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request);
            log.info("Account verified notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send account verified notification", e);
        }
    }

    /**
     * Send account verified notification with email
     */
    public void sendAccountVerifiedNotification(Long userId, String userEmail, String userName, boolean sendEmail) {
        Map<String, String> variables = new HashMap<>();
        variables.put("userName", userName);

        CreateNotificationFromTemplateRequest request = CreateNotificationFromTemplateRequest.builder()
                .templateCode("ACCOUNT_VERIFIED")
                .userId(userId)
                .variables(variables)
                .build();

        try {
            notificationService.createFromTemplate(request, userEmail, sendEmail);
            log.info("Account verified notification sent to user: {} with email: {}", userId, sendEmail);
        } catch (Exception e) {
            log.error("Failed to send account verified notification", e);
        }
    }

    // ================== TICKET ACTION REQUEST NOTIFICATIONS ==================

    /**
     * Send notification when user submits a ticket action request (to admin)
     */
    public void sendTicketActionSubmittedToAdmin(Long adminId, String pnr, String ticketNo,
                                                  String actionType, String userName, Long requestId) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId(adminId)
                .type(NotificationType.TICKET_ACTION_SUBMITTED)
                .priority(NotificationPriority.HIGH)
                .title("New Ticket Action Request")
                .message(String.format("User %s has submitted a %s request for PNR: %s, Ticket: %s",
                        userName, actionType, pnr, ticketNo != null ? ticketNo : "N/A"))
                .actionUrl("/admin/ticket-actions/" + requestId)
                .actionLabel("View Request")
                .referenceId(requestId.toString())
                .referenceType("TICKET_ACTION_REQUEST")
                .build();

        try {
            notificationService.createNotification(request);
            log.info("Ticket action submitted notification sent to admin: {}", adminId);
        } catch (Exception e) {
            log.error("Failed to send ticket action submitted notification to admin", e);
        }
    }

    /**
     * Send notification when admin sends a quote to user
     */
    public void sendTicketActionQuotedToUser(Long userId, String userEmail, String pnr, Long bookingId,
                                              String actionType, String totalAmount, String currency,
                                              Long requestId, boolean sendEmail) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId(userId)
                .type(NotificationType.TICKET_ACTION_QUOTED)
                .priority(NotificationPriority.HIGH)
                .title("Ticket Action Quote Received")
                .message(String.format("Your %s request for PNR: %s has been quoted at %s %s. Please review and confirm.",
                        actionType, pnr, totalAmount, currency))
                .actionUrl("dashboard/bookings/" + bookingId)
                .actionLabel("View Quote")
                .referenceId(requestId.toString())
                .referenceType("TICKET_ACTION_REQUEST")
                .build();

        try {
            notificationService.createNotification(request, userEmail, sendEmail);
            log.info("Ticket action quoted notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send ticket action quoted notification to user", e);
        }
    }

    /**
     * Send notification when admin rejects user's request
     */
    public void sendTicketActionRejectedToUser(Long userId, String userEmail, String pnr, String ticketNo,
                                                String actionType, String reason, Long requestId, boolean sendEmail) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId(userId)
                .type(NotificationType.TICKET_ACTION_REJECTED)
                .priority(NotificationPriority.MEDIUM)
                .title("Ticket Action Request Rejected")
                .message(String.format("Your %s request for PNR: %s has been rejected. Reason: %s",
                        actionType, pnr, reason != null ? reason : "No reason provided"))
                .actionUrl("/bookings/ticket-actions/" + requestId)
                .actionLabel("View Details")
                .referenceId(requestId.toString())
                .referenceType("TICKET_ACTION_REQUEST")
                .build();

        try {
            notificationService.createNotification(request, userEmail, sendEmail);
            log.info("Ticket action rejected notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send ticket action rejected notification to user", e);
        }
    }

    /**
     * Send notification when user confirms quote (to admin)
     */
    public void sendTicketActionConfirmedToAdmin(Long adminId, String pnr, String ticketNo,
                                                  String actionType, String userName, Long requestId) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId(adminId)
                .type(NotificationType.TICKET_ACTION_CONFIRMED)
                .priority(NotificationPriority.HIGH)
                .title("Ticket Action Quote Confirmed")
                .message(String.format("User %s has confirmed the %s quote for PNR: %s. Ready for processing.",
                        userName, actionType, pnr))
                .actionUrl("/admin/ticket-actions/" + requestId)
                .actionLabel("Process Request")
                .referenceId(requestId.toString())
                .referenceType("TICKET_ACTION_REQUEST")
                .build();

        try {
            notificationService.createNotification(request);
            log.info("Ticket action confirmed notification sent to admin: {}", adminId);
        } catch (Exception e) {
            log.error("Failed to send ticket action confirmed notification to admin", e);
        }
    }

    /**
     * Send notification when admin starts processing (to user)
     */
    public void sendTicketActionProcessingToUser(Long userId, String userEmail, String pnr,
                                                  String actionType, Long requestId, boolean sendEmail,Long bookingId) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId(userId)
                .type(NotificationType.TICKET_ACTION_PROCESSING)
                .priority(NotificationPriority.MEDIUM)
                .title("Ticket Action In Progress")
                .message(String.format("Your %s request for PNR: %s is now being processed. We'll notify you once completed.",
                        actionType, pnr))
                .actionUrl("dashboard/bookings/" + bookingId)
                .actionLabel("View Status")
                .referenceId(requestId.toString())
                .referenceType("TICKET_ACTION_REQUEST")
                .build();

        try {
            notificationService.createNotification(request, userEmail, sendEmail);
            log.info("Ticket action processing notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send ticket action processing notification to user", e);
        }
    }

    /**
     * Send notification when admin completes the request (to user)
     */
    public void sendTicketActionCompletedToUser(Long userId, String userEmail, String pnr,
                                                 String actionType, String finalResult,
                                                 Long requestId, boolean sendEmail,Long bookingId) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId(userId)
                .type(NotificationType.TICKET_ACTION_COMPLETED)
                .priority(NotificationPriority.HIGH)
                .title("Ticket Action Completed")
                .message(String.format("Your %s request for PNR: %s has been completed. %s",
                        actionType, pnr, finalResult != null ? finalResult : ""))
                .actionUrl("dashboard/bookings/ticket-actions/" + bookingId)

                .actionLabel("View Details")
                .referenceId(requestId.toString())
                .referenceType("TICKET_ACTION_REQUEST")
                .build();

        try {
            notificationService.createNotification(request, userEmail, sendEmail);
            log.info("Ticket action completed notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send ticket action completed notification to user", e);
        }
    }

    /**
     * Send notification when ticket action fails (to user)
     */
    public void sendTicketActionFailedToUser(Long userId, String userEmail, String pnr,
                                              String actionType, String reason,
                                              Long requestId, boolean sendEmail) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId(userId)
                .type(NotificationType.TICKET_ACTION_FAILED)
                .priority(NotificationPriority.HIGH)
                .title("Ticket Action Failed")
                .message(String.format("Your %s request for PNR: %s has failed. %s",
                        actionType, pnr, reason != null ? reason : "Please contact support for details."))
                .actionUrl("/bookings/ticket-actions/" + requestId)
                .actionLabel("View Details")
                .referenceId(requestId.toString())
                .referenceType("TICKET_ACTION_REQUEST")
                .build();

        try {
            notificationService.createNotification(request, userEmail, sendEmail);
            log.info("Ticket action failed notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send ticket action failed notification to user", e);
        }
    }

    public void sendDailyReport(DailyReportResponseDTO report) {
        Map<String, String> variables = new HashMap<>();

        // Booking
        variables.put("totalBooking", String.valueOf(report.getTotalBooking()));
        variables.put("totalBookingProcessed", String.valueOf(report.getTotalBookingProcessed()));
        variables.put("totalBookingPnr", String.valueOf(report.getTotalBookingPnr()));
        variables.put("totalBookingConfirmed", String.valueOf(report.getTotalBookingConfirmed()));
        variables.put("totalBookingCanceled", String.valueOf(report.getTotalBookingCanceled()));
        variables.put("totalBookingCBooked", String.valueOf(report.getTotalBookingCBooked()));
        variables.put("totalBookingTicketed", String.valueOf(report.getTotalBookingTicketed()));
        variables.put("totalBookingOnHold", String.valueOf(report.getTotalBookingOnHold()));
        variables.put("totalBookingVoided", String.valueOf(report.getTotalBookingVoided()));
        variables.put("totalBookingTicketIssued", String.valueOf(report.getTotalBookingTicketIssued()));

        // Pending Deposits
        variables.put("totalPendingDepositBdt", safe(report.getTotalPendingDepositBdt()));
        variables.put("totalPendingDepositInr", safe(report.getTotalPendingDepositInr()));
        variables.put("totalPendingDepositUsd", safe(report.getTotalPendingDepositUsd()));
        variables.put("totalPendingDepositPkr", safe(report.getTotalPendingDepositPkr()));
        variables.put("totalPendingDepositSar", safe(report.getTotalPendingDepositSar()));
        variables.put("totalPendingDepositQar", safe(report.getTotalPendingDepositQar()));

        // Approved Deposits
        variables.put("totalApprovedDepositBdt", safe(report.getTotalApprovedDepositBdt()));
        variables.put("totalApprovedDepositInr", safe(report.getTotalApprovedDepositInr()));
        variables.put("totalApprovedDepositUsd", safe(report.getTotalApprovedDepositUsd()));
        variables.put("totalApprovedDepositPkr", safe(report.getTotalApprovedDepositPkr()));
        variables.put("totalApprovedDepositSar", safe(report.getTotalApprovedDepositSar()));
        variables.put("totalApprovedDepositQar", safe(report.getTotalApprovedDepositQar()));

        // Users
        variables.put("totalUserCreated", String.valueOf(report.getTotalUserCreated()));
        variables.put("totalAgencyCreated", String.valueOf(report.getTotalAgencyCreated()));

        CreateNotificationFromTemplateRequest request =
                CreateNotificationFromTemplateRequest.builder()
                        .templateCode("DAILY_REPORT")
                        .businessId(null)
                        .userId(null)
                        .variables(variables)
                        .createdBy(0L)
                        .build();
        try {
            notificationService.createFromTemplate(request, "info@kingstartravel.com", true);
            log.info("Daily report send to admin");
        } catch (Exception e) {
            log.error("Failed to send daily report notification", e);
        }
    }

    private String safe(BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }
}
