package com.aerionsoft.application.service.audit;

import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.enums.audit.ActivityOutcome;
import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.util.ActorContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ActivityAdminAuditSupport {

    private final ActivityLogService activityLogService;
    private final ActivityAgencyContextSupport agencyContextSupport;

    public ActivityAdminAuditSupport(
            ActivityLogService activityLogService,
            ActivityAgencyContextSupport agencyContextSupport) {
        this.activityLogService = activityLogService;
        this.agencyContextSupport = agencyContextSupport;
    }

    public void logDepositDecision(Long depositId, Long userId, DepositStatus status, Double amount, String adminRemarks) {
        ActivityEventType eventType = status == DepositStatus.APPROVED
                ? ActivityEventType.DEPOSIT_APPROVED
                : ActivityEventType.DEPOSIT_REJECTED;

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("targetUserId", userId);
        metadata.put("amount", amount);
        metadata.put("status", status.name());
        if (adminRemarks != null && !adminRemarks.isBlank()) {
            metadata.put("adminRemarks", adminRemarks);
        }
        agencyContextSupport.enrichMetadataFromUserId(metadata, userId);

        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(eventType)
                .outcome(ActivityOutcome.SUCCESS)
                .actor(ActorContext.current())
                .resourceType("WALLET_DEPOSIT")
                .resourceId(String.valueOf(depositId))
                .metadata(metadata)
                .build());
    }

    public void logCreditLimitChange(Long businessId, BigDecimal amount, String cause) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("businessId", businessId);
        metadata.put("amount", amount);
        if (cause != null && !cause.isBlank()) {
            metadata.put("cause", cause);
        }
        agencyContextSupport.resolveFromBusinessId(businessId)
                .ifPresent(snapshot -> agencyContextSupport.putSnapshot(metadata, snapshot));

        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(ActivityEventType.CREDIT_LIMIT_CHANGED)
                .outcome(ActivityOutcome.SUCCESS)
                .actor(ActorContext.current())
                .resourceType("BUSINESS")
                .resourceId(String.valueOf(businessId))
                .metadata(metadata)
                .build());
    }

    public void logCreditRequestDecision(Long requestId, Long businessId, ActivityEventType eventType, String adminRemarks) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("businessId", businessId);
        if (adminRemarks != null && !adminRemarks.isBlank()) {
            metadata.put("adminRemarks", adminRemarks);
        }
        agencyContextSupport.resolveFromBusinessId(businessId)
                .ifPresent(snapshot -> agencyContextSupport.putSnapshot(metadata, snapshot));

        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(eventType)
                .outcome(ActivityOutcome.SUCCESS)
                .actor(ActorContext.current())
                .resourceType("CREDIT_REQUEST")
                .resourceId(String.valueOf(requestId))
                .metadata(metadata)
                .build());
    }

    public void logBalanceMovement(
            ActivityEventType eventType,
            Long userId,
            Double amount,
            String currency,
            String reason,
            String reference,
            String sourceType,
            Long sourceId) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("targetUserId", userId);
        metadata.put("amount", amount);
        if (currency != null && !currency.isBlank()) {
            metadata.put("currency", currency);
        }
        if (reason != null && !reason.isBlank()) {
            metadata.put("reason", reason);
        }
        if (reference != null && !reference.isBlank()) {
            metadata.put("reference", reference);
        }
        if (sourceType != null && !sourceType.isBlank()) {
            metadata.put("sourceType", sourceType);
        }
        if (sourceId != null) {
            metadata.put("sourceId", sourceId);
        }
        agencyContextSupport.enrichMetadataFromUserId(metadata, userId);

        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(eventType)
                .outcome(ActivityOutcome.SUCCESS)
                .actor(ActorContext.current())
                .resourceType("WALLET")
                .resourceId(userId != null ? String.valueOf(userId) : null)
                .metadata(metadata)
                .build());
    }
}
