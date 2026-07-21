package com.aerionsoft.application.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthDebugFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        System.out.println("=== AUTH DEBUG ===");
        System.out.println("URL: " + request.getRequestURI());
        System.out.println("Method: " + request.getMethod());

        if (auth != null) {
//            System.out.println("Authenticated: " + auth.isAuthenticated());
//            System.out.println("Principal: " + auth.getPrincipal());
//            System.out.println("Authorities: " + auth.getAuthorities());
//            System.out.println("Details: " + auth.getDetails());

            auth.getAuthorities().forEach(authority -> {
                System.out.println("  - Authority: " + authority.getAuthority() +
                        " (Type: " + authority.getClass().getName() + ")");
            });
        } else {
            System.out.println("Authentication is NULL");
        }
        System.out.println("==================");

        filterChain.doFilter(request, response);
    }
}