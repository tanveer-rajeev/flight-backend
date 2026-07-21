package com.aerionsoft.application.util;

import com.aerionsoft.application.config.SecurityConfig;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * Mirrors {@link SecurityConfig} permitAll rules so audit aspects
 * can skip public routes.
 */
public final class AuthenticatedRouteMatcher {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private static final List<PublicRule> PUBLIC_RULES = List.of(
            new PublicRule(null, "/api/auth/**"),
            new PublicRule(null, "/api/oauth/**"),
            new PublicRule(null, "/api/admin/auth/**"),
            new PublicRule(null, "/css/**"),
            new PublicRule(null, "/js/**"),
            new PublicRule(null, "/images/**"),
            new PublicRule(null, "/api/admin/airport-airline/**"),
            new PublicRule(null, "/api/flight/common/**"),
            new PublicRule(null, "/api/flights/**"),
            new PublicRule(null, "/api/core/callback/**"),
            new PublicRule(null, "/api/webhooks/stripe"),
            new PublicRule(HttpMethod.GET, "/api/public/**"),
            new PublicRule(HttpMethod.POST, "/api/public/businesses"),
            new PublicRule(HttpMethod.POST, "/api/public/files/upload/image"),
            new PublicRule(HttpMethod.POST, "/api/payments/**"),
            new PublicRule(null, "/health"),
            new PublicRule(null, "/ws/**")
    );

    private static final List<String> READ_LIKE_PATH_FRAGMENTS = List.of(
            "/filter",
            "/search",
            "/get-reservation",
            "/load-booking",
            "/heartbeat"
    );

    private AuthenticatedRouteMatcher() {
    }

    public static boolean isPublicRoute(String path, String httpMethod) {
        if (path == null || path.isBlank()) {
            return true;
        }
        HttpMethod method = parseMethod(httpMethod);
        for (PublicRule rule : PUBLIC_RULES) {
            if (rule.matches(path, method)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isReadLikeMutation(String path) {
        if (path == null) {
            return false;
        }
        String normalized = path.toLowerCase();
        return READ_LIKE_PATH_FRAGMENTS.stream().anyMatch(normalized::contains);
    }

    private static HttpMethod parseMethod(String httpMethod) {
        if (httpMethod == null || httpMethod.isBlank()) {
            return null;
        }
        try {
            return HttpMethod.valueOf(httpMethod.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private record PublicRule(HttpMethod method, String pattern) {
        boolean matches(String path, HttpMethod requestMethod) {
            if (method != null && requestMethod != null && method != requestMethod) {
                return false;
            }
            return MATCHER.match(pattern, path);
        }
    }
}
