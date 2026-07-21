package com.aerionsoft.application.config;

import com.aerionsoft.application.annotation.AuditedAction;
import com.aerionsoft.application.annotation.SkipAutoAudit;
import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.enums.audit.ActivityOutcome;
import com.aerionsoft.application.service.audit.ActivityLogService;
import com.aerionsoft.application.util.ActorContext;
import com.aerionsoft.application.util.AuthenticatedRouteMatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catch-all audit for authenticated controller mutations not covered by {@link AuditedAction}
 * or domain-specific audit support classes.
 */
@Aspect
@Component
@Order(3)
@Slf4j
public class ControllerMutationAuditAspect {

    private final ActivityLogService activityLogService;

    public ControllerMutationAuditAspect(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @Around("execution(* com.aerionsoft.application.controller..*(..))")
    public Object auditControllerMutation(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        if (!shouldAudit(method, joinPoint)) {
            return joinPoint.proceed();
        }

        ActorContext actor = ActorContext.current();
        HttpServletRequest request = currentRequest();
        String path = request != null ? request.getRequestURI() : null;
        String httpMethod = request != null ? request.getMethod() : null;

        try {
            Object result = joinPoint.proceed();
            activityLogService.log(buildEntry(actor, ActivityOutcome.SUCCESS, joinPoint, path, httpMethod, null));
            return result;
        } catch (Throwable throwable) {
            activityLogService.log(buildEntry(actor, ActivityOutcome.FAILURE, joinPoint, path, httpMethod, throwable.getMessage()));
            throw throwable;
        }
    }

    private boolean shouldAudit(Method method, ProceedingJoinPoint joinPoint) {
        if (!isMutationMethod(method)) {
            return false;
        }
        if (method.isAnnotationPresent(AuditedAction.class)) {
            return false;
        }
        if (method.isAnnotationPresent(SkipAutoAudit.class)) {
            return false;
        }
        Class<?> declaringClass = joinPoint.getTarget().getClass();
        if (declaringClass.isAnnotationPresent(SkipAutoAudit.class)) {
            return false;
        }
        if (!ActorContext.current().isAuthenticated()) {
            return false;
        }

        HttpServletRequest request = currentRequest();
        if (request == null) {
            return false;
        }

        String path = request.getRequestURI();
        String httpMethod = request.getMethod();
        if (AuthenticatedRouteMatcher.isPublicRoute(path, httpMethod)) {
            return false;
        }
        return !AuthenticatedRouteMatcher.isReadLikeMutation(path);
    }

    private ActivityLogService.ActivityLogEntry buildEntry(
            ActorContext actor,
            ActivityOutcome outcome,
            ProceedingJoinPoint joinPoint,
            String path,
            String httpMethod,
            String failureReason) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("controller", signature.getDeclaringType().getSimpleName());
        metadata.put("method", signature.getName());
        if (httpMethod != null) {
            metadata.put("httpMethod", httpMethod);
        }
        if (path != null) {
            metadata.put("path", path);
        }
        if (failureReason != null) {
            metadata.put("reason", failureReason);
        }

        return ActivityLogService.ActivityLogEntry.builder()
                .eventType(ActivityEventType.ADMIN_ACTION)
                .outcome(outcome)
                .actor(actor)
                .description(signature.getDeclaringType().getSimpleName() + "." + signature.getName())
                .metadata(metadata)
                .build();
    }

    private static boolean isMutationMethod(Method method) {
        if (method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class)) {
            return true;
        }
        RequestMapping mapping = method.getAnnotation(RequestMapping.class);
        if (mapping == null) {
            return false;
        }
        org.springframework.web.bind.annotation.RequestMethod[] methods = mapping.method();
        if (methods.length == 0) {
            return true;
        }
        for (org.springframework.web.bind.annotation.RequestMethod requestMethod : methods) {
            if (requestMethod == org.springframework.web.bind.annotation.RequestMethod.POST
                    || requestMethod == org.springframework.web.bind.annotation.RequestMethod.PUT
                    || requestMethod == org.springframework.web.bind.annotation.RequestMethod.PATCH
                    || requestMethod == org.springframework.web.bind.annotation.RequestMethod.DELETE) {
                return true;
            }
        }
        return false;
    }

    private static HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }
}
