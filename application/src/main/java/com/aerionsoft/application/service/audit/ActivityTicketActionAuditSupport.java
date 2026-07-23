package com.aerionsoft.application.service.audit;

import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.Booking.TicketActionRequest;
import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.enums.audit.ActivityOutcome;
import com.aerionsoft.application.util.ActorContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ActivityTicketActionAuditSupport {

    private static final String RESOURCE_TYPE = "TICKET_ACTION_REQUEST";

    private final ActivityLogService activityLogService;
    private final ActivityAgencyContextSupport agencyContextSupport;

    public ActivityTicketActionAuditSupport(
            ActivityLogService activityLogService,
            ActivityAgencyContextSupport agencyContextSupport) {
        this.activityLogService = activityLogService;
        this.agencyContextSupport = agencyContextSupport;
    }

    public void logSubmitted(TicketActionRequest request) {
        log(request, ActivityEventType.TICKET_ACTION_SUBMITTED, ActivityOutcome.SUCCESS, null);
    }

    public void logQuoted(TicketActionRequest request, Long adminUserId) {
        log(request, ActivityEventType.TICKET_ACTION_QUOTED, ActivityOutcome.SUCCESS,
                ActorContext.forAdmin(adminUserId, null));
    }

    public void logUserConfirmed(TicketActionRequest request) {
        log(request, ActivityEventType.TICKET_ACTION_USER_CONFIRMED, ActivityOutcome.SUCCESS, null);
    }

    public void logRejected(TicketActionRequest request, Long adminUserId, boolean autoExpired) {
        Map<String, Object> extra = new LinkedHashMap<>();
        if (autoExpired) {
            extra.put("autoExpired", true);
        }
        ActorContext actor = autoExpired
                ? ActorContext.system()
                : (adminUserId != null ? ActorContext.forAdmin(adminUserId, null) : ActorContext.current());
        log(request, ActivityEventType.TICKET_ACTION_REJECTED, ActivityOutcome.SUCCESS, actor, extra);
    }

    public void logProcessingStarted(TicketActionRequest request, Long adminUserId) {
        log(request, ActivityEventType.TICKET_ACTION_PROCESSING_STARTED, ActivityOutcome.SUCCESS,
                ActorContext.forAdmin(adminUserId, null));
    }

    public void logFinalized(TicketActionRequest request, Long adminUserId, boolean completed) {
        ActivityEventType eventType = completed
                ? ActivityEventType.TICKET_ACTION_COMPLETED
                : ActivityEventType.TICKET_ACTION_FAILED;
        Map<String, Object> extra = new LinkedHashMap<>();
        if (request.getFinalResult() != null && !request.getFinalResult().isBlank()) {
            extra.put("finalResult", request.getFinalResult());
        }
        if (request.getExternalReference() != null && !request.getExternalReference().isBlank()) {
            extra.put("externalReference", request.getExternalReference());
        }
        if (request.getSupplierRefundCost() != null) {
            extra.put("supplierRefundCost", request.getSupplierRefundCost());
        }
        if (request.getReissueDate() != null) {
            extra.put("reissueDate", request.getReissueDate().toString());
        }
        log(request, eventType, ActivityOutcome.SUCCESS, ActorContext.forAdmin(adminUserId, null), extra);
    }

    private void log(
            TicketActionRequest request,
            ActivityEventType eventType,
            ActivityOutcome outcome,
            ActorContext actorOverride) {
        log(request, eventType, outcome, actorOverride, Map.of());
    }

    private void log(
            TicketActionRequest request,
            ActivityEventType eventType,
            ActivityOutcome outcome,
            ActorContext actorOverride,
            Map<String, Object> extraMetadata) {
        if (request == null || request.getId() == null) {
            return;
        }

        Booking booking = request.getBooking();
        Map<String, Object> metadata = buildMetadata(request, booking);
        if (extraMetadata != null && !extraMetadata.isEmpty()) {
            metadata.putAll(extraMetadata);
        }

        ActorContext actor = actorOverride != null ? actorOverride : ActorContext.current();

        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(eventType)
                .outcome(outcome)
                .actor(actor)
                .resourceType(RESOURCE_TYPE)
                .resourceId(String.valueOf(request.getId()))
                .description(eventType.getDefaultDescription())
                .metadata(metadata)
                .build());
    }

    private Map<String, Object> buildMetadata(TicketActionRequest request, Booking booking) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("ticketActionRequestId", request.getId());
        metadata.put("ticketActionType", request.getType() != null ? request.getType().name() : null);
        metadata.put("status", request.getStatus() != null ? request.getStatus().name() : null);
        if (booking != null) {
            agencyContextSupport.enrichMetadataFromBooking(metadata, booking);
        }
        if (request.getQuoteTotalAmount() != null) {
            metadata.put("quoteTotalAmount", request.getQuoteTotalAmount());
        }
        if (request.getQuoteCurrency() != null) {
            metadata.put("quoteCurrency", request.getQuoteCurrency());
        }
        if (request.getReason() != null && !request.getReason().isBlank()) {
            metadata.put("reason", request.getReason());
        }
        return metadata;
    }
}
