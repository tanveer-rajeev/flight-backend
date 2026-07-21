package com.aerionsoft.application.util;

import com.aerionsoft.application.enums.audit.ActorType;
import com.aerionsoft.application.service.user.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ActorContextTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void current_returnsGuestWhenUnauthenticated() {
        ActorContext actor = ActorContext.current();
        assertEquals(ActorType.GUEST, actor.getType());
        assertNull(actor.getId());
    }

    @Test
    void current_returnsAdminActorForAdminProvider() {
        CustomUserDetails principal = new CustomUserDetails(
                7L,
                "admin@test.com",
                "secret",
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_admin")),
                "admin");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        ActorContext actor = ActorContext.current();
        assertEquals(ActorType.ADMIN, actor.getType());
        assertEquals(7L, actor.getId());
        assertEquals("admin@test.com", actor.getEmail());
    }

    @Test
    void current_returnsUserActorWithImpersonationClaim() {
        CustomUserDetails principal = new CustomUserDetails(
                42L,
                "user@test.com",
                "secret",
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_user")),
                "user",
                3L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        ActorContext actor = ActorContext.current();
        assertEquals(ActorType.USER, actor.getType());
        assertEquals(42L, actor.getId());
        assertEquals(3L, actor.getImpersonatedByAdminId());
    }
}
