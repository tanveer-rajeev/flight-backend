package com.aerionsoft.application.dto.flight;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MarkupContext {
    private String userType; // GUEST, USER, AGENT
    private Long userId;
    private Long businessId;
    private boolean isAuthenticated;
    private boolean isAgent;

    public static MarkupContext guest() {
        return MarkupContext.builder()
                .userType("GUEST")
                .isAuthenticated(false)
                .isAgent(false)
                .build();
    }

    public static MarkupContext authenticatedUser(Long userId) {
        return MarkupContext.builder()
                .userType("USER")
                .userId(userId)
                .isAuthenticated(true)
                .isAgent(false)
                .build();
    }

    public static MarkupContext agent(Long userId, Long businessId) {
        return MarkupContext.builder()
                .userType("AGENT")
                .userId(userId)
                .businessId(businessId)
                .isAuthenticated(true)
                .isAgent(true)
                .build();
    }
}

