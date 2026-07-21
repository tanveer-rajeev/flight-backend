package com.aerionsoft.application.config;

import com.aerionsoft.application.annotation.AuditedAction;
import com.aerionsoft.application.enums.audit.ActivityOutcome;
import com.aerionsoft.application.service.audit.ActivityLogService;
import com.aerionsoft.application.util.ActorContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Aspect
@Component
@Order(2)
@Slf4j
public class AuditedActionAspect {

    private final ActivityLogService activityLogService;

    public AuditedActionAspect(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @Around("@annotation(auditedAction)")
    public Object logAuditedAction(ProceedingJoinPoint joinPoint, AuditedAction auditedAction) throws Throwable {
        ActorContext actor = ActorContext.current();
        try {
            Object result = joinPoint.proceed();
            activityLogService.log(buildEntry(auditedAction, actor, ActivityOutcome.SUCCESS, joinPoint, null));
            return result;
        } catch (Throwable throwable) {
            if (auditedAction.logOnFailure()) {
                activityLogService.log(buildEntry(auditedAction, actor, ActivityOutcome.FAILURE, joinPoint, throwable.getMessage()));
            }
            throw throwable;
        }
    }

    private ActivityLogService.ActivityLogEntry buildEntry(
            AuditedAction auditedAction,
            ActorContext actor,
            ActivityOutcome outcome,
            ProceedingJoinPoint joinPoint,
            String failureReason) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (failureReason != null) {
            metadata.put("reason", failureReason);
        }

        return ActivityLogService.ActivityLogEntry.builder()
                .eventType(auditedAction.value())
                .outcome(outcome)
                .actor(actor)
                .resourceType(blankToNull(auditedAction.resourceType()))
                .resourceId(resolveResourceId(joinPoint, auditedAction.resourceIdParam()))
                .description(blankToNull(auditedAction.description()))
                .metadata(metadata.isEmpty() ? null : metadata)
                .build();
    }

    private String resolveResourceId(ProceedingJoinPoint joinPoint, String paramName) {
        if (paramName == null || paramName.isBlank()) {
            return null;
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] names = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (names == null || args == null) {
            return null;
        }
        for (int i = 0; i < names.length && i < args.length; i++) {
            if (paramName.equals(names[i]) && args[i] != null) {
                return String.valueOf(args[i]);
            }
        }
        return null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
