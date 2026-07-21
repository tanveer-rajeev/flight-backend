package com.aerionsoft.application.repository.chat;

import com.aerionsoft.application.entity.chat.ChatMessage;
import com.aerionsoft.application.enums.chat.ChatSenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByConversation_IdOrderByIdDesc(Long conversationId, Pageable pageable);

    Page<ChatMessage> findByConversation_IdAndIsInternalFalseOrderByIdDesc(Long conversationId, Pageable pageable);

    List<ChatMessage> findByConversation_IdOrderByIdAsc(Long conversationId);

    List<ChatMessage> findByConversation_IdAndIsInternalFalseOrderByIdAsc(Long conversationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ChatMessage m
            SET m.isRead = true, m.readAt = :readAt
            WHERE m.conversation.id = :conversationId
              AND m.senderType = :senderType
              AND m.isRead = false
              AND m.isInternal = false
            """)
    int markUnreadPublicAsRead(@Param("conversationId") Long conversationId,
                               @Param("senderType") ChatSenderType senderType,
                               @Param("readAt") LocalDateTime readAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ChatMessage m
            SET m.isRead = true, m.readAt = :readAt
            WHERE m.conversation.id = :conversationId
              AND m.senderType = :senderType
              AND m.isRead = false
            """)
    int markUnreadAsRead(@Param("conversationId") Long conversationId,
                         @Param("senderType") ChatSenderType senderType,
                         @Param("readAt") LocalDateTime readAt);
}
