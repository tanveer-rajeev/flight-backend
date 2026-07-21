package com.aerionsoft.application.filters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtUtilImpersonationTest {

    private final JwtUtil jwtUtil = new JwtUtil();

    @Test
    void generateImpersonationToken_includesAdminClaim() {
        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        "user@test.com",
                        "password",
                        java.util.List.of());

        String token = jwtUtil.generateImpersonationToken(userDetails, 99L, false);

        assertEquals("user", jwtUtil.extractProvider(token));
        assertEquals(99L, jwtUtil.extractImpersonatedByAdminId(token));
    }

    @Test
    void extractImpersonatedByAdminId_returnsNullForRegularToken() {
        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        "user@test.com",
                        "password",
                        java.util.List.of());

        String token = jwtUtil.generateToken(userDetails, "user", false);

        assertNull(jwtUtil.extractImpersonatedByAdminId(token));
    }
}
