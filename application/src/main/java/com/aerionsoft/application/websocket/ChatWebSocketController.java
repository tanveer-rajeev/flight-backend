package com.aerionsoft.application.websocket;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.chat.ChatMessageDTO;
import com.aerionsoft.application.dto.chat.ChatWsReadRequest;
import com.aerionsoft.application.dto.chat.ChatWsSendRequest;
import com.aerionsoft.application.dto.chat.ChatWsTypingRequest;
import com.aerionsoft.application.dto.chat.SendChatMessageRequest;
import com.aerionsoft.application.enums.chat.ChatSenderType;
import com.aerionsoft.application.service.chat.ChatService;
import com.aerionsoft.application.service.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final WebSocketAuthService webSocketAuthService;

    @MessageMapping(WebSocketTopics.APP_CHAT_SEND)
    @SendToUser(WebSocketTopics.QUEUE_CHAT)
    public BaseResponse<ChatMessageDTO> send(@Payload ChatWsSendRequest request, Authentication authentication) {
        CustomUserDetails details = requirePrincipal(authentication);
        if (request == null || request.getConversationId() == null) {
            throw new IllegalArgumentException("conversationId is required");
        }
        SendChatMessageRequest body = SendChatMessageRequest.builder()
                .body(request.getBody())
                .attachments(request.getAttachments())
                .isInternal(request.getIsInternal())
                .build();

        ChatMessageDTO message;
        if (webSocketAuthService.isAdminUser(authentication)) {
            message = chatService.sendAdminMessage(request.getConversationId(), details.getId(), body);
        } else if (webSocketAuthService.isClientPresenceUser(authentication)) {
            if (Boolean.TRUE.equals(request.getIsInternal())) {
                throw new AccessDeniedException("Users cannot create internal notes");
            }
            message = chatService.sendUserMessage(request.getConversationId(), details.getId(), body);
        } else {
            throw new AccessDeniedException("Unsupported token for live chat");
        }
        return BaseResponse.ok("Message sent", message);
    }

    @MessageMapping(WebSocketTopics.APP_CHAT_TYPING)
    public void typing(@Payload ChatWsTypingRequest request, Authentication authentication) {
        CustomUserDetails details = requirePrincipal(authentication);
        if (request == null || request.getConversationId() == null) {
            return;
        }
        ChatSenderType senderType;
        if (webSocketAuthService.isAdminUser(authentication)) {
            senderType = ChatSenderType.ADMIN;
        } else if (webSocketAuthService.isClientPresenceUser(authentication)) {
            senderType = ChatSenderType.USER;
        } else {
            throw new AccessDeniedException("Unsupported token for live chat");
        }
        chatService.publishTyping(request.getConversationId(), senderType, details.getId(), request.isTyping());
    }

    @MessageMapping(WebSocketTopics.APP_CHAT_READ)
    public void read(@Payload ChatWsReadRequest request, Authentication authentication) {
        CustomUserDetails details = requirePrincipal(authentication);
        if (request == null || request.getConversationId() == null) {
            return;
        }
        if (webSocketAuthService.isAdminUser(authentication)) {
            chatService.markReadByAdmin(request.getConversationId(), details.getId());
        } else if (webSocketAuthService.isClientPresenceUser(authentication)) {
            chatService.markReadByUser(request.getConversationId(), details.getId());
        } else {
            throw new AccessDeniedException("Unsupported token for live chat");
        }
    }

    private CustomUserDetails requirePrincipal(Authentication authentication) {
        return webSocketAuthService.resolvePrincipal(authentication)
                .orElseThrow(() -> new AccessDeniedException("WebSocket authentication required"));
    }
}
