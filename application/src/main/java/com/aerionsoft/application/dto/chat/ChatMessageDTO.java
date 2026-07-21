package com.aerionsoft.application.dto.chat;

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
public class ChatMessageDTO {

    private Long id;
    private Long conversationId;
    private ChatSenderType senderType;
    private Long senderId;
    private String senderName;
    private String body;
    /** Raw attachment URLs (images and/or voice). */
    private List<String> attachments;
    /** Typed view of {@link #attachments} for UI (IMAGE / VOICE). */
    private List<ChatAttachmentDTO> media;
    private Boolean isInternal;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private String createdTimeOffset;
}
