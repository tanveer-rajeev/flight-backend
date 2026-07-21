package com.aerionsoft.application.util;

import org.slf4j.MDC;

import java.util.UUID;

public final class TraceIdSupport {

    private static final String CORRELATION_ID = "cid";

    private TraceIdSupport() {
    }

    public static String currentTraceId() {
        String cid = MDC.get(CORRELATION_ID);
        if (cid == null || cid.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return cid;
    }
}
