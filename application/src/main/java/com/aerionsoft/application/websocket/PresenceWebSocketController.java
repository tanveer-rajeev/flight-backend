package com.aerionsoft.application.websocket;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.service.user.ActiveUserPresenceService;
import com.aerionsoft.application.service.user.CustomUserDetails;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class PresenceWebSocketController {

    private final ActiveUserPresenceService presenceService;
    private final WebSocketAuthService webSocketAuthService;

    public PresenceWebSocketController(ActiveUserPresenceService presenceService,
                                       WebSocketAuthService webSocketAuthService) {
        this.presenceService = presenceService;
        this.webSocketAuthService = webSocketAuthService;
    }

    @MessageMapping(WebSocketTopics.APP_PRESENCE_HEARTBEAT)
    @SendToUser(WebSocketTopics.QUEUE_PRESENCE)
    public BaseResponse<Void> heartbeat(Authentication authentication, SimpMessageHeaderAccessor accessor) {
        if (!webSocketAuthService.isClientPresenceUser(authentication)) {
            throw new IllegalStateException("Only client tokens may send presence heartbeat");
        }
        CustomUserDetails details = webSocketAuthService.resolvePrincipal(authentication)
                .orElseThrow(() -> new IllegalStateException("Not authenticated"));

        presenceService.recordActivity(
                details,
                WebSocketPresenceHelper.clientIp(accessor),
                WebSocketPresenceHelper.userAgent(accessor));

        return BaseResponse.ok("Presence heartbeat");
    }
}
