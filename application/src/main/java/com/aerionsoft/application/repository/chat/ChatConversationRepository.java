package com.aerionsoft.application.repository.chat;

import com.aerionsoft.application.entity.chat.ChatConversation;
import com.aerionsoft.application.enums.chat.ChatConversationStatus;
import com.aerionsoft.application.enums.chat.ChatSenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findFirstByUser_IdAndStatusInOrderByCreatedAtDesc(
            Long userId, Collection<ChatConversationStatus> statuses);

    Page<ChatConversation> findByUser_IdOrderByLastMessageAtDescCreatedAtDesc(Long userId, Pageable pageable);

    Page<ChatConversation> findByStatusOrderByLastMessageAtDescCreatedAtDesc(
            ChatConversationStatus status, Pageable pageable);

    Page<ChatConversation> findAllByOrderByLastMessageAtDescCreatedAtDesc(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ChatConversation c
            SET c.status = :activeStatus,
                c.assignedAdminId = :adminId,
                c.updatedAt = CURRENT_TIMESTAMP
            WHERE c.id = :id AND c.status = :openStatus
            """)
    int claimIfOpen(@Param("id") Long id,
                    @Param("adminId") Long adminId,
                    @Param("activeStatus") ChatConversationStatus activeStatus,
                    @Param("openStatus") ChatConversationStatus openStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ChatConversation c
            SET c.status = :openStatus,
                c.assignedAdminId = NULL,
                c.updatedAt = CURRENT_TIMESTAMP
            WHERE c.id = :id
              AND c.status = :activeStatus
              AND c.assignedAdminId = :adminId
            """)
    int releaseIfOwned(@Param("id") Long id,
                       @Param("adminId") Long adminId,
                       @Param("openStatus") ChatConversationStatus openStatus,
                       @Param("activeStatus") ChatConversationStatus activeStatus);

    @Query("""
            SELECT COUNT(m) FROM ChatMessage m
            WHERE m.conversation.id = :conversationId
              AND m.senderType = :senderType
              AND m.isRead = false
              AND m.isInternal = false
            """)
    long countUnreadPublic(@Param("conversationId") Long conversationId,
                           @Param("senderType") ChatSenderType senderType);

    @Query("""
            SELECT COUNT(m) FROM ChatMessage m
            WHERE m.conversation.id = :conversationId
              AND m.senderType = :senderType
              AND m.isRead = false
            """)
    long countUnread(@Param("conversationId") Long conversationId,
                     @Param("senderType") ChatSenderType senderType);

    List<ChatConversation> findByIdIn(Collection<Long> ids);
}
