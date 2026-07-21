package com.aerionsoft.application.controller;

import com.aerionsoft.application.service.user.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

public class BaseController {

    public String getProviderName(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getProvider();
        }
        return null;
    }

    public boolean isAdmin() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        return authentication.getAuthorities().stream()
                .anyMatch(auth ->
                        auth.getAuthority().equals("ROLE_accountent") ||
                                auth.getAuthority().equals("ROLE_admin") || auth.getAuthority().equals("ROLE_bd-accountant")
                );
    }

    public Long getUserIdFromAuthentication() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getId();
        }
        return null;
    }

    protected Long currentUserId() {
        return getUserIdFromAuthentication();
    }

    public boolean bypassBillingIfVlife()
    {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_vlife"));
    }

    public static String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            // First IP is the real client
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

}
