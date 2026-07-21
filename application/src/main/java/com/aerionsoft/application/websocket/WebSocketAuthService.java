package com.aerionsoft.application.websocket;

import com.aerionsoft.application.enums.booking.ServiceProviderEnum;
import com.aerionsoft.application.filters.JwtUtil;
import com.aerionsoft.application.service.user.CustomUserDetails;
import com.aerionsoft.application.service.user.UserDetailsServiceImpl;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WebSocketAuthService {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    public WebSocketAuthService(JwtUtil jwtUtil, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    public Optional<Authentication> authenticateToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        String jwt = rawToken.startsWith("Bearer ") ? rawToken.substring(7).trim() : rawToken.trim();

        try {
            String username = jwtUtil.extractUsername(jwt);
            String provider = jwtUtil.extractProvider(jwt);

            if (provider == null || !ServiceProviderEnum.isValidProvider(provider)) {
                return Optional.empty();
            }

            UserDetails userDetails = userDetailsService.loadUserByEmail(username, provider);
            if (!jwtUtil.validateToken(jwt, userDetails)) {
                return Optional.empty();
            }

            Claims claims = jwtUtil.getPayload(jwt);
            List<GrantedAuthority> authorities = extractRoles(claims).stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            return Optional.of(new UsernamePasswordAuthenticationToken(userDetails, null, authorities));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<CustomUserDetails> resolvePrincipal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails details) {
            return Optional.of(details);
        }
        return Optional.empty();
    }

    /** Presence WebSocket is for client app tokens only (provider {@code user}). */
    public boolean isClientPresenceUser(Authentication authentication) {
        return resolvePrincipal(authentication)
                .map(details -> "user".equalsIgnoreCase(details.getProvider()))
                .orElse(false);
    }

    /** Admin dashboard WebSocket (provider {@code admin}). */
    public boolean isAdminUser(Authentication authentication) {
        return resolvePrincipal(authentication)
                .map(details -> "admin".equalsIgnoreCase(details.getProvider()))
                .orElse(false);
    }

    private List<String> extractRoles(Claims claims) {
        Object authoritiesClaim = claims.get("authorities");
        if (authoritiesClaim instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
