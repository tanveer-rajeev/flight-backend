package com.aerionsoft.application.util;

import com.aerionsoft.application.enums.audit.ActorType;
import com.aerionsoft.application.service.user.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Resolves the current actor from the security context, distinguishing users from admins.
 */
public final class ActorContext {

    private static final ActorContext GUEST = new ActorContext(ActorType.GUEST, null, null, null);
    private static final ActorContext SYSTEM = new ActorContext(ActorType.SYSTEM, null, null, null);

    private final ActorType type;
    private final Long id;
    private final String email;
    private final Long impersonatedByAdminId;

    public ActorContext(ActorType type, Long id, String email, Long impersonatedByAdminId) {
        this.type = type;
        this.id = id;
        this.email = email;
        this.impersonatedByAdminId = impersonatedByAdminId;
    }

    public static ActorContext guest() {
        return GUEST;
    }

    public static ActorContext system() {
        return SYSTEM;
    }

    public static ActorContext current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return guest();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails details) {
            ActorType actorType = "admin".equalsIgnoreCase(details.getProvider())
                    ? ActorType.ADMIN
                    : ActorType.USER;
            return new ActorContext(
                    actorType,
                    details.getId(),
                    details.getUsername(),
                    details.getImpersonatedByAdminId());
        }

        if (principal instanceof String s && !"anonymousUser".equals(s)) {
            return new ActorContext(ActorType.GUEST, null, s, null);
        }

        return guest();
    }

    public static ActorContext forUser(Long userId, String email) {
        return new ActorContext(ActorType.USER, userId, email, null);
    }

    public static ActorContext forAdmin(Long adminId, String email) {
        return new ActorContext(ActorType.ADMIN, adminId, email, null);
    }

    public ActorType getType() {
        return type;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Long getImpersonatedByAdminId() {
        return impersonatedByAdminId;
    }

    public boolean isAuthenticated() {
        return type == ActorType.USER || type == ActorType.ADMIN;
    }
}
