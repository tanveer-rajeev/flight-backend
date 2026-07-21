package com.aerionsoft.application.dto.chat;

import com.aerionsoft.application.enums.chat.ChatConversationStatus;
import com.aerionsoft.application.enums.chat.ChatSenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversationDTO {

    private Long id;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private Long businessId;
    private Long assignedAdminId;
    private String assignedAdminName;
    private ChatConversationStatus status;
    private String subject;
    private LocalDateTime lastMessageAt;
    private String lastMessagePreview;
    private long unreadCount;
    private LocalDateTime closedAt;
    private ChatSenderType closedByType;
    private Long closedById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChatMessageDTO> messages;
}
