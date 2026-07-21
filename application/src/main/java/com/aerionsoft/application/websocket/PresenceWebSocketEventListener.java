package com.aerionsoft.application.websocket;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.service.user.ActiveUserPresenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@Slf4j
public class PresenceWebSocketEventListener {

    private final ActiveUserPresenceService presenceService;
    private final WebSocketAuthService webSocketAuthService;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceWebSocketEventListener(ActiveUserPresenceService presenceService,
                                          WebSocketAuthService webSocketAuthService,
                                          SimpMessagingTemplate messagingTemplate) {
        this.presenceService = presenceService;
        this.webSocketAuthService = webSocketAuthService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication authentication = resolveAuthentication(accessor);
        webSocketAuthService.resolvePrincipal(authentication).ifPresent(details -> {
            presenceService.markOnline(
                    details,
                    WebSocketPresenceHelper.clientIp(accessor),
                    WebSocketPresenceHelper.userAgent(accessor));
            sendPresenceAck(details.getUsername(), "online");
            log.debug("Presence online via WebSocket: {} ({})", details.getUsername(), details.getProvider());
        });
    }

    @EventListener
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication authentication = resolveAuthentication(accessor);
        webSocketAuthService.resolvePrincipal(authentication).ifPresent(details -> {
            presenceService.markOffline(details);
            log.debug("Presence offline via WebSocket: {} ({})", details.getUsername(), details.getProvider());
        });
    }

    private Authentication resolveAuthentication(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication authentication) {
            return authentication;
        }
        return null;
    }

    private void sendPresenceAck(String username, String status) {
        messagingTemplate.convertAndSendToUser(
                username,
                WebSocketTopics.QUEUE_PRESENCE,
                BaseResponse.ok("Presence " + status));
    }
}
