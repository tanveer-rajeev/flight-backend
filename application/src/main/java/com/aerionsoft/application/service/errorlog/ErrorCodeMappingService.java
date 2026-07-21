package com.aerionsoft.application.service.errorlog;

import com.aerionsoft.application.enums.common.MicroserviceType;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ErrorCodeMappingService {

    private final Map<String, Map<String, String>> serviceErrorMappings = new HashMap<>();

    public ErrorCodeMappingService() {
        initializeErrorMappings();
    }

    private void initializeErrorMappings() {
        // Core Booking Service Error Codes
        Map<String, String> coreBookingErrors = new HashMap<>();
        coreBookingErrors.put("INVALID_CONTACT", "Please provide a valid contact number between 7 to 17 digits");
        coreBookingErrors.put("CONTACT_NUMERIC_ONLY", "Contact number should contain only numbers");
        coreBookingErrors.put("PASSPORT_EXPIRY", "Passport should be valid for at least 6 months from travel date");
        coreBookingErrors.put("INSUFFICIENT_BALANCE", "Insufficient balance for booking");
        coreBookingErrors.put("BOOKING_FAILED", "Booking could not be processed. Please try again");
        coreBookingErrors.put("INVALID_PASSENGER_DATA", "Invalid passenger information provided");
        coreBookingErrors.put("FLIGHT_NOT_AVAILABLE", "Selected flight is no longer available");
        serviceErrorMappings.put("CORE_BOOKING", coreBookingErrors);

        // Payment Service Error Codes
        Map<String, String> paymentErrors = new HashMap<>();
        paymentErrors.put("PAYMENT_FAILED", "Payment processing failed. Please try again");
        paymentErrors.put("INSUFFICIENT_FUNDS", "Insufficient funds in your account");
        paymentErrors.put("CARD_DECLINED", "Your card was declined. Please try a different payment method");
        paymentErrors.put("PAYMENT_TIMEOUT", "Payment request timed out. Please try again");
        paymentErrors.put("INVALID_CARD", "Invalid card details provided");
        serviceErrorMappings.put("PAYMENT", paymentErrors);

        // User Service Error Codes
        Map<String, String> userErrors = new HashMap<>();
        userErrors.put("USER_NOT_FOUND", "User account not found");
        userErrors.put("INVALID_CREDENTIALS", "Invalid username or password");
        userErrors.put("ACCOUNT_LOCKED", "Your account has been temporarily locked");
        userErrors.put("EMAIL_EXISTS", "An account with this email already exists");
        userErrors.put("WEAK_PASSWORD", "Password does not meet security requirements");
        serviceErrorMappings.put("USER", userErrors);

        // Visa Service Error Codes
        Map<String, String> visaErrors = new HashMap<>();
        visaErrors.put("INVALID_DOCUMENT", "Invalid or missing required documents");
        visaErrors.put("PROCESSING_FAILED", "Visa application processing failed");
        visaErrors.put("COUNTRY_NOT_SUPPORTED", "Visa services not available for selected country");
        visaErrors.put("INCOMPLETE_APPLICATION", "Application form is incomplete");
        serviceErrorMappings.put("VISA", visaErrors);

        // Wallet Service Error Codes
        Map<String, String> walletErrors = new HashMap<>();
        walletErrors.put("INSUFFICIENT_BALANCE", "Insufficient wallet balance");
        walletErrors.put("TRANSACTION_FAILED", "Wallet transaction failed");
        walletErrors.put("DAILY_LIMIT_EXCEEDED", "Daily transaction limit exceeded");
        walletErrors.put("WALLET_LOCKED", "Wallet is temporarily locked");
        serviceErrorMappings.put("WALLET", walletErrors);

        // Notification Service Error Codes
        Map<String, String> notificationErrors = new HashMap<>();
        notificationErrors.put("EMAIL_SEND_FAILED", "Failed to send email notification");
        notificationErrors.put("SMS_SEND_FAILED", "Failed to send SMS notification");
        notificationErrors.put("INVALID_TEMPLATE", "Invalid notification template");
        serviceErrorMappings.put("NOTIFICATION", notificationErrors);
    }

    public String mapErrorCode(MicroserviceType serviceType, String errorCode) {
        return mapErrorCode(serviceType.getServiceName().toUpperCase().replace("-", "_"), errorCode);
    }

    public String mapErrorCode(String serviceName, String errorCode) {
        Map<String, String> serviceErrors = serviceErrorMappings.get(serviceName.toUpperCase());
        if (serviceErrors != null && serviceErrors.containsKey(errorCode.toUpperCase())) {
            return serviceErrors.get(errorCode.toUpperCase());
        }
        return "Service temporarily unavailable. Please try again later.";
    }

    public String mapErrorMessage(MicroserviceType serviceType, String originalMessage) {
        String serviceName = serviceType.getServiceName().toUpperCase().replace("-", "_");

        // For core booking service, parse common error patterns
        if (serviceType == MicroserviceType.CORE_BOOKING) {
            return mapCoreBookingMessage(originalMessage);
        }

        // For other services, return the original message if it's user-friendly, otherwise generic
        if (originalMessage != null && isUserFriendlyMessage(originalMessage)) {
            return originalMessage;
        }

        return "Service temporarily unavailable. Please try again later.";
    }

    private String mapCoreBookingMessage(String originalMessage) {
        if (originalMessage == null) {
            return "Booking failed. Please try again.";
        }

        String lowerMessage = originalMessage.toLowerCase();

        // Map specific error patterns from core booking
        if (lowerMessage.contains("contact number") && lowerMessage.contains("7 to 17 digits")) {
            return "Please provide a valid contact number between 7 to 17 digits";
        }
        if (lowerMessage.contains("numerals in contact number")) {
            return "Contact number should contain only numbers";
        }
        if (lowerMessage.contains("passport") && lowerMessage.contains("6 months")) {
            return "Passport should be valid for at least 6 months from travel date";
        }
        if (lowerMessage.contains("insufficient balance")) {
            return "Insufficient balance for booking";
        }

        // If the message is already user-friendly, return it
        if (isUserFriendlyMessage(originalMessage)) {
            return originalMessage;
        }

        return "Booking could not be processed. Please check your details and try again.";
    }

    private boolean isUserFriendlyMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        String lowerMessage = message.toLowerCase();

        // Avoid technical error messages
        if (lowerMessage.contains("exception") ||
            lowerMessage.contains("error:") ||
            lowerMessage.contains("sql") ||
            lowerMessage.contains("null pointer") ||
            lowerMessage.contains("class cast") ||
            lowerMessage.contains("connection") ||
            lowerMessage.contains("timeout") ||
            lowerMessage.contains("500") ||
            lowerMessage.contains("404")) {
            return false;
        }

        return true;
    }

    public void addServiceErrorMapping(String serviceName, String errorCode, String userMessage) {
        serviceErrorMappings.computeIfAbsent(serviceName.toUpperCase(), k -> new HashMap<>())
                .put(errorCode.toUpperCase(), userMessage);
    }
}
