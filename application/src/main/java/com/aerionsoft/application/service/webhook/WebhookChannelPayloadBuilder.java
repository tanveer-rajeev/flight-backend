package com.aerionsoft.application.service.webhook;

import com.aerionsoft.application.dto.booking.BookingRequest;
import com.aerionsoft.application.dto.booking.core.CoreResponse;
import com.aerionsoft.application.dto.webhook.WebhookAlertMessage;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.webhook.WebhookAlertConfig;
import com.aerionsoft.application.enums.webhook.WebhookAlertType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class WebhookChannelPayloadBuilder {

    private WebhookChannelPayloadBuilder() {
    }

    public static WebhookAlertMessage buildTestMessage(WebhookAlertConfig config) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Config", config.getName());
        fields.put("Alert Type", config.getAlertType().getLabel());
        fields.put("Channel", config.getChannel().getLabel());
        return WebhookAlertMessage.builder()
                .title("Webhook Alert Test")
                .body("This is a test message from TufanTrip admin webhook configuration.")
                .fields(fields)
                .build();
    }

    public static WebhookAlertMessage buildTicketedBookingPostProcessFailureMessage(
            Booking booking,
            String ticketNo,
            String errorMessage) {
        Map<String, String> fields = buildBookingFields(booking, ticketNo, errorMessage);
        fields.put("Alert Type", WebhookAlertType.TICKETED_BOOKING_POST_PROCESS_FAILED.getLabel());

        return buildMessage(
                "Urgent: Ticket Issued — IT Action Required",
                "A ticket was issued at the GDS/core layer but backend post-processing failed. "
                        + "Contact IT immediately to reconcile booking, wallet, and supplier invoice.",
                fields);
    }

    public static WebhookAlertMessage buildBookingCreateCoreFailureMessage(
            BookingRequest request,
            String customerName,
            CoreResponse coreResponse,
            String errorMessage) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Alert Type", WebhookAlertType.BOOKING_CREATE_CORE_FAILED.getLabel());
        fields.put("Operation", "Booking Create");
        fields.put("Customer", customerName != null ? customerName : "Unknown User");
        fields.put("Provider", request != null && request.getProviderName() != null
                ? request.getProviderName().name() : "N/A");
        fields.put("Channel", request != null && request.getChannel() != null ? request.getChannel() : "N/A");
        fields.put("Book Type", request != null && request.getBookType() != null
                ? request.getBookType().name() : "N/A");
        fields.put("Result Index", request != null && request.getResultIndex() != null
                ? request.getResultIndex() : "N/A");
        appendCoreResponseFields(fields, coreResponse);
        fields.put("Error", errorMessage != null ? errorMessage : "Unknown error");

        return buildMessage(
                "Booking Create Failed — Core API Error",
                "Online booking create failed at the core/GDS layer. Review the error and contact IT if needed.",
                fields);
    }

    public static WebhookAlertMessage buildHoldToBookCoreFailureMessage(
            Booking booking,
            CoreResponse coreResponse,
            String errorMessage) {
        Map<String, String> fields = buildBookingFields(booking, null, errorMessage);
        fields.put("Alert Type", WebhookAlertType.HOLD_TO_BOOK_CORE_FAILED.getLabel());
        fields.put("Operation", "Hold-to-Book");
        appendCoreResponseFields(fields, coreResponse);

        return buildMessage(
                "Hold-to-Book Failed — Core API Error",
                "Hold-to-book failed at the core/GDS layer. The PNR may still be on hold — verify with IT.",
                fields);
    }

    private static Map<String, String> buildBookingFields(Booking booking, String ticketNo, String errorMessage) {
        String userName = booking.getCreatedBy() != null
                ? (booking.getCreatedBy().getFullName() != null
                ? booking.getCreatedBy().getFullName()
                : booking.getCreatedBy().getEmail())
                : "Unknown User";
        String pnr = booking.getPnr() != null ? booking.getPnr() : "N/A";
        String provider = booking.getProviderName() != null ? booking.getProviderName().name() : "N/A";
        String channel = booking.getChannel() != null ? booking.getChannel() : "N/A";
        String effectiveTicketNo = ticketNo != null && !ticketNo.isBlank() ? ticketNo : "N/A";

        Map<String, String> fields = new LinkedHashMap<>();
        if (booking.getId() != null) {
            fields.put("Booking ID", String.valueOf(booking.getId()));
        }
        fields.put("Booking Ref", booking.getBookingReference() != null ? booking.getBookingReference() : "N/A");
        fields.put("PNR", pnr);
        fields.put("Ticket", effectiveTicketNo);
        fields.put("Provider", provider);
        fields.put("Channel", channel);
        fields.put("Customer", userName);
        fields.put("Error", errorMessage != null ? errorMessage : "Unknown error");
        return fields;
    }

    private static void appendCoreResponseFields(Map<String, String> fields, CoreResponse coreResponse) {
        if (coreResponse == null) {
            fields.put("Core Status", "N/A");
            return;
        }
        fields.put("Core Status", coreResponse.getStatus() != null ? coreResponse.getStatus().name() : "N/A");
        if (coreResponse.getPnr() != null && !coreResponse.getPnr().isBlank()) {
            fields.put("Core PNR", coreResponse.getPnr());
        }
        if (coreResponse.getMessage() != null && !coreResponse.getMessage().isBlank()) {
            fields.put("Core Message", coreResponse.getMessage());
        }
        if (coreResponse.getReason() != null && !coreResponse.getReason().isBlank()) {
            fields.put("Core Reason", coreResponse.getReason());
        }
    }

    private static WebhookAlertMessage buildMessage(String title, String intro, Map<String, String> fields) {
        String body = fields.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
        return WebhookAlertMessage.builder()
                .title(title)
                .body(intro + "\n\n" + body)
                .fields(fields)
                .build();
    }
}
