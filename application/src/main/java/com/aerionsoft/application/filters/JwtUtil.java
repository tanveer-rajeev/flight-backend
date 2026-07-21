package com.aerionsoft.application.filters;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.*;

@Component
public class JwtUtil {

    @Value("${platform.name}:tufantrip")
    private String platformName;

    private Key getSigningKey() {

        String SECRET_KEY = "uZHGSgKS91yB+yXPaAmH1JuPrr8Wa+1hnHVY+7Sv3UY98hjJhbr97MhMNi6Wxw5NR5y1LFadfM4EYPgNes+jXA==";
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    private static final long ACCESS_TOKEN_TTL_MS = 25L * 60 * 1000; // 25 minutes
    private static final long AGENCY_TOKEN_TTL_MS = 2L * 24 * 60 * 60 * 1000; // 2 days
    private static final long REFRESH_TOKEN_TTL_MS = 7L * 24 * 60 * 60 * 1000; // 7 days

    public String generateToken(UserDetails userDetails, String provider, boolean isAgent) {

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        long tokenTtl = isAgent ? AGENCY_TOKEN_TTL_MS : ACCESS_TOKEN_TTL_MS;

        return Jwts.builder()
                .claim("provider", provider)
                .claim("company", platformName)
                .claim("isAgency", isAgent)
                .claim("authorities", roles)
                .claim("id", UUID.randomUUID().toString())
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + tokenTtl))
                .issuer(platformName + "-auth")
                .audience().add(platformName + "-api").and()
                .signWith(getSigningKey())
                .compact();
    }

    public String generateImpersonationToken(UserDetails userDetails, Long adminUserId, boolean isAgency) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        long tokenTtl = isAgency ? AGENCY_TOKEN_TTL_MS : ACCESS_TOKEN_TTL_MS;

        return Jwts.builder()
                .claim("provider", "user")
                .claim("company", platformName)
                .claim("isAgency", isAgency)
                .claim("authorities", roles)
                .claim("impersonatedByAdminId", adminUserId)
                .claim("id", UUID.randomUUID().toString())
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + tokenTtl))
                .issuer(platformName + "-auth")
                .audience().add(platformName + "-api").and()
                .signWith(getSigningKey())
                .compact();
    }

    public String generateToken(UserDetails userDetails, String provider, boolean isAgent, String clientId) {

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();


        return Jwts.builder()
                .claim("provider", provider)
                .claim("company", platformName)
                .claim("isAgency", isAgent)
                .claim("authorities", roles)
                .claim("id", clientId)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_TTL_MS))
                .issuer(platformName + "-auth")
                .audience().add(platformName + "-api").and()
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return parseAndValidate(token).getSubject();
    }


    public Claims getPayload(String token) {
        return parseAndValidate(token);
    }

    private Claims parseAndValidate(String token) {

        return Jwts.parser()
                .requireIssuer(platformName + "-auth")
                .requireAudience(platformName + "-api")
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    public boolean validateToken(String token, UserDetails userDetails) {

        Claims claims = parseAndValidate(token);

        return claims.getSubject().equals(userDetails.getUsername())
                && userDetails.isEnabled()
                && userDetails.isAccountNonLocked()
                && userDetails.isCredentialsNonExpired();
    }


    public String extractProvider(String token) {
        return (String) Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("provider");
    }

    public Long extractImpersonatedByAdminId(String token) {
        try {
            Object value = getPayload(token).get("impersonatedByAdminId");
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String s && !s.isBlank()) {
                return Long.parseLong(s);
            }
        } catch (Exception ignored) {
            // not an impersonation token
        }
        return null;
    }

    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .claim("type", "refresh")
                .claim("company", platformName)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_TTL_MS))
                .issuer(platformName + "-auth")
                .audience().add(platformName + "-api").and()
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseAndValidate(token);
            return "refresh".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    public long getRefreshTokenTtlMs() {
        return REFRESH_TOKEN_TTL_MS;
    }

}

