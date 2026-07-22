package com.aerionsoft.application.service.audit;

import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.enums.audit.ActivityOutcome;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.util.ActorContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ActivityBookingAuditSupport {

    private final ActivityLogService activityLogService;

    public ActivityBookingAuditSupport(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    public void logBookingCreated(
            Long bookingId,
            String pnr,
            BookingStatus status,
            String sourceType,
            Long ownerUserId,
            String provider) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("pnr", pnr);
        metadata.put("status", status != null ? status.name() : null);
        metadata.put("sourceType", sourceType);
        metadata.put("ownerUserId", ownerUserId);
        metadata.put("provider", provider);

        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(ActivityEventType.BOOKING_CREATED)
                .outcome(ActivityOutcome.SUCCESS)
                .actor(ActorContext.current())
                .resourceType("BOOKING")
                .resourceId(String.valueOf(bookingId))
                .metadata(metadata)
                .build());
    }

    public void logStatusChange(
            Long bookingId,
            String pnr,
            BookingStatus oldStatus,
            BookingStatus newStatus,
            String reason,
            Map<String, Object> extraMetadata) {
        if (newStatus == null || newStatus == oldStatus) {
            return;
        }

        ActivityEventType eventType = resolveStatusEventType(oldStatus, newStatus);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("pnr", pnr);
        metadata.put("oldStatus", oldStatus != null ? oldStatus.name() : null);
        metadata.put("newStatus", newStatus.name());
        if (reason != null && !reason.isBlank()) {
            metadata.put("reason", reason);
        }
        if (extraMetadata != null) {
            metadata.putAll(extraMetadata);
        }

        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(eventType)
                .outcome(ActivityOutcome.SUCCESS)
                .actor(ActorContext.current())
                .resourceType("BOOKING")
                .resourceId(String.valueOf(bookingId))
                .description(eventType.getDefaultDescription())
                .metadata(metadata)
                .build());
    }

    /**
     * Dedicated audit for admin refund: append-only wallet credit + supplier reverse.
     * Does not delete original PURCHASE ledger rows.
     */
    public void logAdminRefund(
            Long bookingId,
            String pnr,
            BookingStatus oldStatus,
            String reason,
            Map<String, Object> refundMetadata) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("pnr", pnr);
        metadata.put("oldStatus", oldStatus != null ? oldStatus.name() : null);
        metadata.put("newStatus", BookingStatus.REFUND.name());
        metadata.put("channel", "ADMIN_REFUND");
        metadata.put("appendOnly", true);
        if (reason != null && !reason.isBlank()) {
            metadata.put("reason", reason);
        }
        if (refundMetadata != null) {
            metadata.putAll(refundMetadata);
        }

        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(ActivityEventType.BOOKING_REFUNDED)
                .outcome(ActivityOutcome.SUCCESS)
                .actor(ActorContext.current())
                .resourceType("BOOKING")
                .resourceId(String.valueOf(bookingId))
                .description("Admin booking refund processed")
                .metadata(metadata)
                .build());
    }

    /**
     * Field-level audit for admin booking edit / agency transfer.
     * Spec: {@code BookingServiceAdminEditTest} (pending implementation approval).
     */
    public void logAdminEdit(
            Long bookingId,
            String pnr,
            String reason,
            Map<String, Object> changes) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("pnr", pnr);
        metadata.put("channel", "ADMIN_EDIT");
        metadata.put("appendOnly", true);
        if (reason != null && !reason.isBlank()) {
            metadata.put("reason", reason);
        }
        if (changes != null) {
            metadata.put("changes", changes);
        }

        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(ActivityEventType.BOOKING_UPDATED)
                .outcome(ActivityOutcome.SUCCESS)
                .actor(ActorContext.current())
                .resourceType("BOOKING")
                .resourceId(String.valueOf(bookingId))
                .description("Admin booking edit processed")
                .metadata(metadata)
                .build());
    }

    public void logBookingDeleted(Long bookingId, String pnr) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("pnr", pnr);

        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(ActivityEventType.BOOKING_DELETED)
                .outcome(ActivityOutcome.SUCCESS)
                .actor(ActorContext.current())
                .resourceType("BOOKING")
                .resourceId(String.valueOf(bookingId))
                .metadata(metadata)
                .build());
    }

    public void logBookingUpdated(Long bookingId, String pnr) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("pnr", pnr);

        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(ActivityEventType.BOOKING_UPDATED)
                .outcome(ActivityOutcome.SUCCESS)
                .actor(ActorContext.current())
                .resourceType("BOOKING")
                .resourceId(String.valueOf(bookingId))
                .metadata(metadata)
                .build());
    }

    private ActivityEventType resolveStatusEventType(BookingStatus oldStatus, BookingStatus newStatus) {
        if (newStatus == BookingStatus.CANCELLED) {
            return ActivityEventType.BOOKING_CANCELLED;
        }
        if (newStatus == BookingStatus.REFUND) {
            return ActivityEventType.BOOKING_REFUNDED;
        }
        if (newStatus == BookingStatus.REISSUE) {
            return ActivityEventType.BOOKING_REISSUED;
        }
        if (newStatus == BookingStatus.VOID) {
            return ActivityEventType.BOOKING_VOIDED;
        }
        if ((newStatus == BookingStatus.TICKET_ISSUED || newStatus == BookingStatus.TICKETED)
                && oldStatus != BookingStatus.TICKET_ISSUED
                && oldStatus != BookingStatus.TICKETED) {
            return ActivityEventType.TICKET_ISSUED;
        }
        return ActivityEventType.BOOKING_STATUS_CHANGED;
    }
}
