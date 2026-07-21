package com.aerionsoft.application.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedRouteMatcherTest {

    @Test
    void publicAuthRoutesAreExcluded() {
        assertTrue(AuthenticatedRouteMatcher.isPublicRoute("/api/auth/login", "POST"));
        assertTrue(AuthenticatedRouteMatcher.isPublicRoute("/api/admin/auth/login", "POST"));
        assertTrue(AuthenticatedRouteMatcher.isPublicRoute("/api/flights/v1/search", "POST"));
        assertTrue(AuthenticatedRouteMatcher.isPublicRoute("/api/public/businesses", "POST"));
        assertTrue(AuthenticatedRouteMatcher.isPublicRoute("/api/public/files/upload/image", "POST"));
        assertTrue(AuthenticatedRouteMatcher.isPublicRoute("/api/public/country/list", "GET"));
        assertFalse(AuthenticatedRouteMatcher.isPublicRoute("/api/public/businesses", "PUT"));
    }

    @Test
    void authenticatedRoutesAreNotPublic() {
        assertFalse(AuthenticatedRouteMatcher.isPublicRoute("/api/bookings/create", "POST"));
        assertFalse(AuthenticatedRouteMatcher.isPublicRoute("/api/admin/users/1", "PUT"));
        assertFalse(AuthenticatedRouteMatcher.isPublicRoute("/api/wallet/admin/charge", "POST"));
    }

    @Test
    void readLikeMutationsAreDetected() {
        assertTrue(AuthenticatedRouteMatcher.isReadLikeMutation("/api/bookings/get-reservation"));
        assertTrue(AuthenticatedRouteMatcher.isReadLikeMutation("/api/admin/bookings/filter"));
        assertFalse(AuthenticatedRouteMatcher.isReadLikeMutation("/api/bookings/create"));
    }
}
