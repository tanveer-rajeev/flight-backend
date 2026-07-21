package com.aerionsoft.application.dto.chat;

import com.aerionsoft.application.enums.chat.ChatConversationStatus;
import com.aerionsoft.application.enums.chat.ChatSenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Real-time event pushed over STOMP ({@code /user/queue/chat} or {@code /topic/admin/chat/inbox}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRealtimeEvent {

    public enum EventType {
        CONVERSATION_CREATED,
        CONVERSATION_CLAIMED,
        CONVERSATION_RELEASED,
        CONVERSATION_CLOSED,
        MESSAGE,
        INTERNAL_NOTE,
        TYPING,
        READ
    }

    private EventType type;
    private Long conversationId;
    private ChatConversationStatus status;
    private Long assignedAdminId;
    private String assignedAdminName;
    private ChatMessageDTO message;
    private ChatSenderType typingSenderType;
    private Long typingSenderId;
    private boolean typing;
    private ChatSenderType readByType;
    private Long readById;
    private ChatConversationDTO conversation;
}
