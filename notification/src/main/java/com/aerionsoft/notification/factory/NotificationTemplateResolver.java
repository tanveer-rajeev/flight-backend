package com.aerionsoft.notification.factory;

import com.aerionsoft.notification.dto.NotificationType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NotificationTemplateResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");

    private static final Map<String, String> TITLE_TEMPLATES = Map.ofEntries(
            Map.entry("BOOKING_CONFIRMED", "Booking Confirmed"),
            Map.entry("BOOKING_CANCELLED", "Booking Cancelled"),
            Map.entry("BOOKING_TICKET_ISSUED", "Ticket Issued"),
            Map.entry("BOOKING_PRICE_CHANGED", "Price Changed"),
            Map.entry("PAYMENT_SUCCESS", "Payment Successful"),
            Map.entry("PAYMENT_FAILED", "Payment Failed"),
            Map.entry("WALLET_DEPOSIT_CREATED", "Deposit Requested"),
            Map.entry("WALLET_DEPOSIT_APPROVED", "Deposit Approved"),
            Map.entry("WALLET_DEPOSIT_CANCELLED", "Deposit Cancelled"),
            Map.entry("CREDIT_REQUESTED", "Credit Requested"),
            Map.entry("CREDIT_APPROVED", "Credit Approved"),
            Map.entry("CREDIT_REJECTED", "Credit Rejected"),
            Map.entry("BUSINESS_CREATED", "Business Created"),
            Map.entry("BUSINESS_APPROVED", "Business Approved"),
            Map.entry("BUSINESS_REJECTED", "Business Rejected"),
            Map.entry("AGENCY_APPROVED", "Agency Approved"),
            Map.entry("AGENCY_REJECTED", "Agency Rejected"),
            Map.entry("VISA_APPLICATION_UPDATE", "Visa Application Update"),
            Map.entry("TOUR_APPLICATION_UPDATE", "Tour Application Update"),
            Map.entry("ACCOUNT_UPDATED", "Account Updated"),
            Map.entry("ACCOUNT_VERIFIED", "Account Verified"),
            Map.entry("ACCOUNT_WELCOME_USER", "Welcome!"),
            Map.entry("ACCOUNT_LOGIN_ALERT", "New Login Detected"),
            Map.entry("PROMOTION_OFFER", "New Offer For You"),
            Map.entry("SYSTEM_ALERT", "System Alert"),
            Map.entry("SYSTEM_GENERAL", "Notification")
    );

    private static final Map<String, String> BODY_TEMPLATES = Map.ofEntries(
            Map.entry("BOOKING_CONFIRMED", "Your booking {bookingId} has been confirmed."),
            Map.entry("BOOKING_CANCELLED", "Your booking {bookingId} has been cancelled."),
            Map.entry("BOOKING_TICKET_ISSUED", "Your ticket for booking {bookingId} has been issued."),
            Map.entry("BOOKING_PRICE_CHANGED", "The price for booking {bookingId} has changed to {amount}."),
            Map.entry("PAYMENT_SUCCESS", "Your payment of {amount} was successful."),
            Map.entry("PAYMENT_FAILED", "Your payment of {amount} failed. Please try again."),
            Map.entry("WALLET_DEPOSIT_CREATED", "Your deposit request of {amount} has been submitted."),
            Map.entry("WALLET_DEPOSIT_APPROVED", "Your deposit of {amount} has been approved."),
            Map.entry("WALLET_DEPOSIT_CANCELLED", "Your deposit of {amount} has been cancelled."),
            Map.entry("CREDIT_REQUESTED", "Your credit request {creditId} has been submitted."),
            Map.entry("CREDIT_APPROVED", "Your credit request {creditId} has been approved."),
            Map.entry("CREDIT_REJECTED", "Your credit request {creditId} has been rejected."),
            Map.entry("BUSINESS_CREATED", "Business {businessName} has been created."),
            Map.entry("BUSINESS_APPROVED", "Business {businessName} has been approved."),
            Map.entry("BUSINESS_REJECTED", "Business {businessName} has been rejected."),
            Map.entry("AGENCY_APPROVED", "Your agency application has been approved."),
            Map.entry("AGENCY_REJECTED", "Your agency application has been rejected."),
            Map.entry("VISA_APPLICATION_UPDATE", "Your visa application {applicationId} has an update: {status}."),
            Map.entry("TOUR_APPLICATION_UPDATE", "Your tour application {applicationId} has an update: {status}."),
            Map.entry("ACCOUNT_UPDATED", "Your account details have been updated."),
            Map.entry("ACCOUNT_VERIFIED", "Your account has been verified."),
            Map.entry("ACCOUNT_WELCOME_USER", "Welcome aboard, {userName}!"),
            Map.entry("ACCOUNT_LOGIN_ALERT", "A new login was detected from {device} at {location}."),
            Map.entry("PROMOTION_OFFER", "{message}"),
            Map.entry("SYSTEM_ALERT", "{message}"),
            Map.entry("SYSTEM_GENERAL", "{message}")
    );

    public String resolveTitle(NotificationType type, Map<String, Object> metadata) {
        String template = TITLE_TEMPLATES.getOrDefault(type.getCode(), type.getCode());
        return applyPlaceholders(template, metadata);
    }

    public String resolveBody(NotificationType type, Map<String, Object> metadata) {
        String template = BODY_TEMPLATES.getOrDefault(type.getCode(), "");
        return applyPlaceholders(template, metadata);
    }

    private String applyPlaceholders(String template, Map<String, Object> metadata) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = metadata.get(key);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value != null ? value.toString() : ""));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
