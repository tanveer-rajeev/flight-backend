package com.aerionsoft.application.websocket;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
public class ActiveUsersWebSocketListener {

    private final ActiveUsersWebSocketBroadcaster broadcaster;

    public ActiveUsersWebSocketListener(ActiveUsersWebSocketBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination != null && destination.startsWith(WebSocketTopics.TOPIC_ADMIN_ACTIVE_USERS)) {
            broadcaster.onAdminSubscribed();
        }
    }

    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination != null && destination.startsWith(WebSocketTopics.TOPIC_ADMIN_ACTIVE_USERS)) {
            broadcaster.onAdminUnsubscribed();
        }
    }
}
