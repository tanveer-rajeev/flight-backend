package com.aerionsoft.application.util;

import org.slf4j.Logger;
import org.slf4j.MDC;

/**
 * Lightweight structured logging helper. Enriches SLF4J MDC for the duration of
 * each log call so logback patterns can emit correlation and context fields.
 */
public final class StructuredLog {

    public static final String SEVERITY = "severity";
    public static final String EVENT = "event";

    private StructuredLog() {
    }

    public static void info(Logger log, String event, Object... keyValues) {
        logWithLevel(log, "INFO", event, keyValues);
    }

    public static void warn(Logger log, String event, Object... keyValues) {
        logWithLevel(log, "WARN", event, keyValues);
    }

    public static void error(Logger log, String event, Throwable throwable, Object... keyValues) {
        withContext("ERROR", event, keyValues, () -> log.error(formatMessage(event, keyValues), throwable));
    }

    public static void error(Logger log, String event, Object... keyValues) {
        logWithLevel(log, "ERROR", event, keyValues);
    }

    /** Maps to ERROR level with severity=CRITICAL in MDC for alerting filters. */
    public static void critical(Logger log, String event, Throwable throwable, Object... keyValues) {
        withContext("CRITICAL", event, keyValues, () -> log.error(formatMessage(event, keyValues), throwable));
    }

    private static void logWithLevel(Logger log, String level, String event, Object... keyValues) {
        withContext(level, event, keyValues, () -> {
            String message = formatMessage(event, keyValues);
            switch (level) {
                case "WARN" -> log.warn(message);
                case "ERROR" -> log.error(message);
                default -> log.info(message);
            }
        });
    }

    private static void withContext(String severity, String event, Object[] keyValues, Runnable action) {
        MDC.put(SEVERITY, severity);
        MDC.put(EVENT, event);
        applyKeyValues(keyValues);
        try {
            action.run();
        } finally {
            MDC.remove(SEVERITY);
            MDC.remove(EVENT);
            clearKeyValues(keyValues);
        }
    }

    private static void applyKeyValues(Object... keyValues) {
        if (keyValues == null) {
            return;
        }
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            if (key != null) {
                MDC.put(String.valueOf(key), String.valueOf(keyValues[i + 1]));
            }
        }
    }

    private static void clearKeyValues(Object... keyValues) {
        if (keyValues == null) {
            return;
        }
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            if (key != null) {
                MDC.remove(String.valueOf(key));
            }
        }
    }

    private static String formatMessage(String event, Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return "event=" + event;
        }
        StringBuilder builder = new StringBuilder("event=").append(event);
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            builder.append(' ').append(keyValues[i]).append('=').append(keyValues[i + 1]);
        }
        return builder.toString();
    }
}
