package com.aerionsoft.application.service.audit;

import com.aerionsoft.application.dto.audit.ActivityFeedAgencyInfo;
import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

final class ActivityFeedSummaryBuilder {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ActivityFeedSummaryBuilder() {
    }

    static String build(ActivityEventType eventType, Map<String, Object> metadata) {
        return build(eventType, metadata, null);
    }

    static String build(ActivityEventType eventType, Map<String, Object> metadata, ActivityFeedAgencyInfo agency) {
        Map<String, Object> meta = metadata != null ? metadata : Map.of();
        String agencyLabel = agencyLabel(agency, meta);
        String pnr = stringVal(meta.get("pnr"));
        String type = stringVal(meta.get("ticketActionType"));
        String oldStatus = stringVal(meta.get("oldStatus"));
        String newStatus = stringVal(meta.get("newStatus"));
        String amount = stringVal(meta.get("quoteTotalAmount"));
        if (amount == null) {
            amount = stringVal(meta.get("reissueChargeAmountUsd"));
        }
        if (amount == null) {
            amount = stringVal(meta.get("amount"));
        }

        return switch (eventType) {
            case TICKET_ACTION_SUBMITTED -> join(" · ", agencyLabel,
                    type != null ? type + " submitted" : "Ticket action submitted",
                    pnrLabel(pnr));
            case TICKET_ACTION_QUOTED -> join(" · ", agencyLabel,
                    type != null ? type + " quoted" : "Quote sent",
                    amount != null ? amount + " USD" : null,
                    pnrLabel(pnr));
            case TICKET_ACTION_USER_CONFIRMED -> join(" · ", agencyLabel,
                    type != null ? type + " confirmed" : "Quote confirmed",
                    pnrLabel(pnr));
            case TICKET_ACTION_REJECTED -> join(" · ", agencyLabel,
                    type != null ? type + " rejected" : "Request rejected",
                    pnrLabel(pnr));
            case TICKET_ACTION_PROCESSING_STARTED -> join(" · ", agencyLabel,
                    type != null ? type + " processing" : "Processing started",
                    pnrLabel(pnr));
            case TICKET_ACTION_COMPLETED -> join(" · ", agencyLabel,
                    type != null ? type + " completed" : "Action completed",
                    amount != null ? "charge " + amount + " USD" : null,
                    pnrLabel(pnr));
            case TICKET_ACTION_FAILED -> join(" · ", agencyLabel,
                    type != null ? type + " failed" : "Action failed",
                    pnrLabel(pnr));
            case TICKET_ACTION_CANCELLED -> join(" · ", agencyLabel,
                    type != null ? type + " cancelled" : "Action cancelled",
                    pnrLabel(pnr));
            case BOOKING_CREATED -> join(" · ", agencyLabel, "Booking created", pnrLabel(pnr));
            case BOOKING_STATUS_CHANGED, BOOKING_CANCELLED, BOOKING_REFUNDED, BOOKING_REISSUED,
                 BOOKING_VOIDED, TICKET_ISSUED, BOOKING_UPDATED, BOOKING_DELETED ->
                    join(" · ", agencyLabel, pnrLabel(pnr), statusTransition(oldStatus, newStatus));
            case DEPOSIT_APPROVED -> join(" · ", agencyLabel, "Deposit approved", amountLabel(amount));
            case DEPOSIT_REJECTED -> join(" · ", agencyLabel, "Deposit rejected", amountLabel(amount));
            case BALANCE_CREDIT -> join(" · ", agencyLabel, "Balance credited", amountLabel(amount));
            case BALANCE_DEBIT -> join(" · ", agencyLabel, "Balance debited", amountLabel(amount));
            case CREDIT_LIMIT_CHANGED -> join(" · ", agencyLabel, "Credit limit changed");
            case CREDIT_REQUEST_APPROVED -> join(" · ", agencyLabel, "Credit request approved");
            case CREDIT_REQUEST_REJECTED -> join(" · ", agencyLabel, "Credit request rejected");
            case ADMIN_ACTION -> join(" · ", agencyLabel, "Admin action", adminActionSummary(meta));
            default -> join(" · ", agencyLabel,
                    eventType != null ? eventType.getDefaultDescription() : "Activity event");
        };
    }

    private static String adminActionSummary(Map<String, Object> meta) {
        String actionLabel = stringVal(meta.get("actionLabel"));
        if (actionLabel != null && !actionLabel.isBlank()) {
            return actionLabel;
        }
        String controller = stringVal(meta.get("controller"));
        String method = stringVal(meta.get("method"));
        if (controller != null && method != null) {
            return controller + "." + method;
        }
        return stringVal(meta.get("path"));
    }

    private static String agencyLabel(ActivityFeedAgencyInfo agency, Map<String, Object> meta) {
        if (agency != null && agency.getAgencyName() != null && !agency.getAgencyName().isBlank()) {
            return agency.getAgencyName();
        }
        String fromMeta = stringVal(meta.get("agencyName"));
        return fromMeta != null && !fromMeta.isBlank() ? fromMeta : null;
    }

    static Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(metadataJson, new TypeReference<>() {
            });
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private static String pnrLabel(String pnr) {
        return pnr != null ? "PNR " + pnr : null;
    }

    private static String amountLabel(String amount) {
        return amount != null ? amount : null;
    }

    private static String statusTransition(String oldStatus, String newStatus) {
        if (oldStatus != null && newStatus != null) {
            return oldStatus + " → " + newStatus;
        }
        if (newStatus != null) {
            return newStatus;
        }
        return null;
    }

    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(sep);
            }
            sb.append(part);
        }
        return sb.isEmpty() ? "Activity event" : sb.toString();
    }

    private static String stringVal(Object value) {
        return value != null ? String.valueOf(value) : null;
    }
}
