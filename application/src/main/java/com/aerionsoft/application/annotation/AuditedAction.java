package com.aerionsoft.application.annotation;

import com.aerionsoft.application.enums.audit.ActivityEventType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditedAction {
    ActivityEventType value();

    String resourceType() default "";

    /** Method parameter name holding the resource ID, e.g. {@code depositId}. */
    String resourceIdParam() default "";

    String description() default "";

    boolean logOnFailure() default true;
}
