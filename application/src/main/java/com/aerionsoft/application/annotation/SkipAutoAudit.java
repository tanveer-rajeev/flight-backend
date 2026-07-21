package com.aerionsoft.application.annotation;

import com.aerionsoft.application.config.ControllerMutationAuditAspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Skips {@link ControllerMutationAuditAspect} for methods or controllers
 * that already emit structured activity logs elsewhere (e.g. domain audit support classes).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SkipAutoAudit {
}
