package com.aerionsoft.application.websocket;

import com.aerionsoft.application.service.access.PermissionService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class ActiveUsersWebSocketController {

    private final ActiveUsersWebSocketBroadcaster broadcaster;
    private final WebSocketAuthService webSocketAuthService;
    private final PermissionService permissionService;

    public ActiveUsersWebSocketController(ActiveUsersWebSocketBroadcaster broadcaster,
                                          WebSocketAuthService webSocketAuthService,
                                          PermissionService permissionService) {
        this.broadcaster = broadcaster;
        this.webSocketAuthService = webSocketAuthService;
        this.permissionService = permissionService;
    }

    @MessageMapping(WebSocketTopics.APP_ADMIN_ACTIVE_USERS_REFRESH)
    public void refresh(Authentication authentication) {
        if (authentication == null || !webSocketAuthService.isAdminUser(authentication)) {
            throw new IllegalStateException("Admin token required");
        }
        if (!permissionService.hasPermission(authentication, WebSocketTopics.PERMISSION_VIEW_SUMMERY)) {
            throw new IllegalStateException("Missing permission: view-summery");
        }
        broadcaster.publishActiveUsers();
    }
}
