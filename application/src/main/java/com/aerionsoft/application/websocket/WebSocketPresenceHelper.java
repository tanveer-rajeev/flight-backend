package com.aerionsoft.application.websocket;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.Map;

final class WebSocketPresenceHelper {

    private WebSocketPresenceHelper() {
    }

    static String clientIp(SimpMessageHeaderAccessor accessor) {
        return sessionAttribute(accessor, WebSocketTopics.IP_ATTRIBUTE);
    }

    static String userAgent(SimpMessageHeaderAccessor accessor) {
        return sessionAttribute(accessor, WebSocketTopics.USER_AGENT_ATTRIBUTE);
    }

    private static String sessionAttribute(SimpMessageHeaderAccessor accessor, String key) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes == null) {
            return null;
        }
        Object value = attributes.get(key);
        return value != null ? value.toString() : null;
    }
}
