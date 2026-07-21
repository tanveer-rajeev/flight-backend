package com.aerionsoft.application.service.chat;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.chat.ChatRealtimeEvent;
import com.aerionsoft.application.websocket.WebSocketTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Pushes live-chat events to per-user queues and the shared admin inbox topic.
 * Destination principal names match JWT usernames (email).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(String email, ChatRealtimeEvent event) {
        if (email == null || email.isBlank()) {
            return;
        }
        messagingTemplate.convertAndSendToUser(
                email,
                WebSocketTopics.QUEUE_CHAT,
                BaseResponse.ok(event));
    }

    public void broadcastInbox(ChatRealtimeEvent event) {
        messagingTemplate.convertAndSend(
                WebSocketTopics.TOPIC_ADMIN_CHAT_INBOX,
                BaseResponse.ok(event));
    }
}
