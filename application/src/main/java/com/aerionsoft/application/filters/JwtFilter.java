package com.aerionsoft.application.filters;


import com.aerionsoft.application.enums.booking.ServiceProviderEnum;
import com.aerionsoft.application.service.user.CustomUserDetails;
import com.aerionsoft.application.service.user.UserDetailsServiceImpl;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Order(1)
public class JwtFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;
        String provider = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
                provider = jwtUtil.extractProvider(jwt);

                if (provider != null && !ServiceProviderEnum.isValidProvider(provider)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid provider");
                    return;
                }
            } catch (Exception e) {
                // Token is invalid/expired - log and continue to allow anonymous access for public endpoints
                // The security configuration will handle authorization
                chain.doFilter(request, response);
                return;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsServiceImpl.loadUserByEmail(username, provider);
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    // Extract authorities from JWT token
                    Claims claims = jwtUtil.getPayload(jwt);

                    List<String> roles = extractRoles(claims);

                    List<GrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    Object principal = applyImpersonationClaim(userDetails, jwtUtil.extractImpersonatedByAdminId(jwt));

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            principal, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                }
            } catch (Exception e) {
                // User not found or validation failed - continue without authentication
                // The security configuration will handle authorization
            }
        }
        chain.doFilter(request, response);
    }

    public List<String> extractRoles( Claims claims) {
        Object authoritiesClaim = claims.get("authorities");
        if (authoritiesClaim instanceof List) {
            return ((List<?>) authoritiesClaim).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private Object applyImpersonationClaim(UserDetails userDetails, Long impersonatedByAdminId) {
        if (impersonatedByAdminId == null || !(userDetails instanceof CustomUserDetails details)) {
            return userDetails;
        }
        return new CustomUserDetails(
                details.getId(),
                details.getUsername(),
                details.getPassword(),
                details.isVerified(),
                details.isActive(),
                details.getAuthorities(),
                details.getProvider(),
                impersonatedByAdminId);
    }
}