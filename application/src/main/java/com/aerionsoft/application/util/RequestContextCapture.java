package com.aerionsoft.application.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class RequestContextCapture {

    private RequestContextCapture() {
    }

    public record HttpContext(String ipAddress, String userAgent, String httpMethod, String httpPath) {
    }

    public static HttpContext current() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return new HttpContext(null, null, null, null);
        }

        HttpServletRequest request = attributes.getRequest();
        return new HttpContext(
                resolveClientIp(request),
                truncate(request.getHeader("User-Agent"), 500),
                request.getMethod(),
                truncate(request.getRequestURI(), 500));
    }

    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
