package com.aerionsoft.application.config;

import com.aerionsoft.application.service.user.CustomUserDetails;
import com.aerionsoft.application.util.StructuredLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(1)
@Slf4j
public class ControllerLoggingAspect {

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController com.aerionsoft.application.controller..*)")
    public void restControllers() {
    }

    @Around("restControllers()")
    public Object logControllerCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String event = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        long startedAt = System.currentTimeMillis();
        Long userId = currentUserId();

        StructuredLog.info(log, event + ".start", "userId", userId);
        try {
            Object result = joinPoint.proceed();
            StructuredLog.info(log, event + ".success",
                    "userId", userId,
                    "durationMs", System.currentTimeMillis() - startedAt);
            return result;
        } catch (Throwable throwable) {
            StructuredLog.error(log, event + ".failed", throwable,
                    "userId", userId,
                    "durationMs", System.currentTimeMillis() - startedAt);
            throw throwable;
        }
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }
        return null;
    }
}
