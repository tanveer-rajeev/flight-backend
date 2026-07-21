package com.aerionsoft.application.service.audit;

import com.aerionsoft.application.entity.audit.ActivityLog;
import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.enums.audit.ActivityOutcome;
import com.aerionsoft.application.repository.audit.ActivityLogRepository;
import com.aerionsoft.application.util.ActorContext;
import com.aerionsoft.application.util.RequestContextCapture;
import com.aerionsoft.application.util.TraceIdSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Service
@Slf4j
public class ActivityLogService {

    private static final int MAX_METADATA_LENGTH = 10000;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "oldpassword", "newpassword", "otp", "token",
            "refreshtoken", "accesstoken", "secret", "authorization");

    private final ActivityLogRepository activityLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Executor activityLogExecutor;

    @Value("${activity.log.enabled:true}")
    private boolean enabled;

    @Autowired
    public ActivityLogService(
            ActivityLogRepository activityLogRepository,
            @Qualifier("activityLogExecutor") Executor activityLogExecutor) {
        this.activityLogRepository = activityLogRepository;
        this.activityLogExecutor = activityLogExecutor;
    }

    public void log(ActivityLogEntry entry) {
        if (!enabled || entry == null || entry.eventType == null) {
            return;
        }

        ActivityLogEntry enriched = enrichOnCallerThread(entry);
        Runnable persistTask = () -> persist(enriched);
        try {
            activityLogExecutor.execute(persistTask);
        } catch (RejectedExecutionException rejected) {
            log.warn("Activity log executor saturated; persisting synchronously.", rejected);
            persistTask.run();
        }
    }

    private ActivityLogEntry enrichOnCallerThread(ActivityLogEntry entry) {
        RequestContextCapture.HttpContext http = entry.httpContext != null
                ? entry.httpContext
                : RequestContextCapture.current();
        String traceId = entry.traceId != null ? entry.traceId : TraceIdSupport.currentTraceId();

        return entry.toBuilder()
                .traceId(traceId)
                .ipAddress(entry.ipAddress != null ? entry.ipAddress : http.ipAddress())
                .userAgent(entry.userAgent != null ? entry.userAgent : http.userAgent())
                .httpMethod(entry.httpMethod != null ? entry.httpMethod : http.httpMethod())
                .httpPath(entry.httpPath != null ? entry.httpPath : http.httpPath())
                .httpContext(http)
                .build();
    }

    public void logFailure(ActivityEventType eventType, ActorContext actor, String reason, Map<String, Object> metadata) {
        Map<String, Object> meta = metadata != null ? new LinkedHashMap<>(metadata) : new LinkedHashMap<>();
        if (reason != null) {
            meta.putIfAbsent("reason", reason);
        }
        log(ActivityLogEntry.builder()
                .eventType(eventType)
                .outcome(ActivityOutcome.FAILURE)
                .actor(actor != null ? actor : ActorContext.guest())
                .description(reason)
                .metadata(meta)
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void persist(ActivityLogEntry entry) {
        try {
            ActorContext actor = entry.actor != null ? entry.actor : ActorContext.guest();
            RequestContextCapture.HttpContext http = entry.httpContext != null
                    ? entry.httpContext
                    : new RequestContextCapture.HttpContext(null, null, null, null);

            ActivityLog row = ActivityLog.builder()
                    .eventType(entry.eventType)
                    .eventCategory(entry.eventType.getCategory())
                    .outcome(entry.outcome != null ? entry.outcome : ActivityOutcome.SUCCESS)
                    .actorType(actor.getType())
                    .actorId(actor.getId())
                    .actorEmail(actor.getEmail())
                    .impersonatedByAdminId(actor.getImpersonatedByAdminId())
                    .resourceType(entry.resourceType)
                    .resourceId(entry.resourceId)
                    .description(truncate(
                            entry.description != null ? entry.description : entry.eventType.getDefaultDescription(),
                            MAX_DESCRIPTION_LENGTH))
                    .metadata(toJson(sanitizeMetadata(entry.metadata)))
                    .ipAddress(entry.ipAddress)
                    .userAgent(entry.userAgent)
                    .traceId(entry.traceId)
                    .httpMethod(entry.httpMethod)
                    .httpPath(entry.httpPath)
                    .build();

            activityLogRepository.save(row);
        } catch (Exception e) {
            log.warn("Failed to persist activity log event={}", entry.eventType, e);
        }
    }

    private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            if (SENSITIVE_KEYS.contains(entry.getKey().toLowerCase())) {
                sanitized.put(entry.getKey(), "[REDACTED]");
            } else {
                sanitized.put(entry.getKey(), entry.getValue());
            }
        }
        return sanitized;
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return truncate(objectMapper.writeValueAsString(metadata), MAX_METADATA_LENGTH);
        } catch (Exception e) {
            return truncate(String.valueOf(metadata), MAX_METADATA_LENGTH);
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    @Builder(toBuilder = true)
    public static class ActivityLogEntry {
        private ActivityEventType eventType;
        private ActivityOutcome outcome;
        private ActorContext actor;
        private String resourceType;
        private String resourceId;
        private String description;
        private Map<String, Object> metadata;
        private String ipAddress;
        private String userAgent;
        private String traceId;
        private String httpMethod;
        private String httpPath;
        private RequestContextCapture.HttpContext httpContext;
    }
}
