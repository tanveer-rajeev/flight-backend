package com.aerionsoft.application.filters;

import com.aerionsoft.application.service.user.ActiveUserPresenceService;
import com.aerionsoft.application.service.user.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(2)
public class ActiveUserPresenceFilter extends OncePerRequestFilter {

    private final ActiveUserPresenceService presenceService;

    public ActiveUserPresenceFilter(ActiveUserPresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/ws")
                || path.startsWith("/api/ws")
                || path.startsWith("/api/auth/")
                || path.startsWith("/api/admin/auth/")
                || path.startsWith("/api/oauth/")
                || path.equals("/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUserDetails details) {
            presenceService.recordActivity(
                    details,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent"));
        }
    }
}
