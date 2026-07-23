package com.aerionsoft.application.websocket;

import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import com.aerionsoft.application.service.access.PermissionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final WebSocketAuthService webSocketAuthService;
    private final PermissionService permissionService;

    public StompAuthChannelInterceptor(WebSocketAuthService webSocketAuthService,
                                       PermissionService permissionService) {
        this.webSocketAuthService = webSocketAuthService;
        this.permissionService = permissionService;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Authentication authentication = resolveAuthentication(accessor);
            if (authentication == null) {
                throw new AccessDeniedException("WebSocket authentication required");
            }
            if (!webSocketAuthService.isClientPresenceUser(authentication)
                    && !webSocketAuthService.isAdminUser(authentication)) {
                throw new AccessDeniedException("WebSocket requires a client (user) or admin token");
            }
            accessor.setUser(authentication);
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put(WebSocketAuthHandshakeInterceptor.AUTH_ATTRIBUTE, authentication);
            }
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith(WebSocketTopics.TOPIC_ADMIN_ACTIVE_USERS)) {
                Authentication authentication = requireAdminAuth(accessor, "active users feed");
                if (!permissionService.canViewSummery(authentication)) {
                    denySubscribe(accessor, "Missing permission: view-summery (or admin role)");
                }
            }
            if (destination != null && destination.startsWith(WebSocketTopics.TOPIC_ADMIN_ACTIVITY_FEED)) {
                Authentication authentication = requireAdminAuth(accessor, "activity feed");
                if (!permissionService.canViewActivityLog(authentication)) {
                    denySubscribe(accessor, "Missing permission: view-activity-log (or admin role)");
                }
            }
            if (destination != null && destination.startsWith(WebSocketTopics.TOPIC_ADMIN_CHAT_INBOX)) {
                Authentication authentication = resolveAuthentication(accessor);
                // Shared inbox: any authenticated admin may subscribe (claim/reply still enforced in service).
                if (authentication == null || !webSocketAuthService.isAdminUser(authentication)) {
                    throw new AccessDeniedException("Admin token required for live chat inbox");
                }
            }
        }

        return message;
    }

    private Authentication requireAdminAuth(StompHeaderAccessor accessor, String feedName) {
        Authentication authentication = resolveAuthentication(accessor);
        if (authentication == null || !webSocketAuthService.isAdminUser(authentication)) {
            denySubscribe(accessor, "Admin token required for " + feedName);
        }
        return authentication;
    }

    private void denySubscribe(StompHeaderAccessor accessor, String reason) {
        String destination = accessor.getDestination();
        log.warn("STOMP SUBSCRIBE denied destination={} reason={}", destination, reason);
        throw new AccessDeniedException(reason);
    }

    private Authentication resolveAuthentication(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication authentication) {
            return authentication;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            Object auth = sessionAttributes.get(WebSocketAuthHandshakeInterceptor.AUTH_ATTRIBUTE);
            if (auth instanceof Authentication authentication) {
                return authentication;
            }
        }

        List<String> authorization = accessor.getNativeHeader("Authorization");
        if (authorization == null || authorization.isEmpty()) {
            return null;
        }
        return webSocketAuthService.authenticateToken(authorization.get(0)).orElse(null);
    }
}
