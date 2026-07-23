package com.aerionsoft.application.service.chat;

import com.aerionsoft.application.dto.chat.ChatConversationStatsDTO;
import com.aerionsoft.application.dto.chat.AdminStartChatRequest;
import com.aerionsoft.application.dto.chat.ChatAttachmentDTO;
import com.aerionsoft.application.dto.chat.ChatConversationDTO;
import com.aerionsoft.application.dto.chat.ChatRealtimeEvent;
import com.aerionsoft.application.dto.chat.CreateChatConversationRequest;
import com.aerionsoft.application.dto.chat.SendChatMessageRequest;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.entity.chat.ChatConversation;
import com.aerionsoft.application.entity.chat.ChatMessage;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.enums.chat.ChatConversationStatus;
import com.aerionsoft.application.enums.chat.ChatSenderType;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.repository.chat.ChatConversationRepository;
import com.aerionsoft.application.repository.chat.ChatMessageRepository;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.service.user.ActiveUserPresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long ADMIN_A = 100L;
    private static final Long ADMIN_B = 101L;
    private static final Long CONVERSATION_ID = 1L;

    @Mock private ChatConversationRepository conversationRepository;
    @Mock private ChatMessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private AdminUserRepository adminUserRepository;
    @Mock private ChatRealtimePublisher realtimePublisher;
    @Mock private ActiveUserPresenceService presenceService;
    @Mock private ChatOfflineNotifyService offlineNotifyService;
    @Mock private ChatConversationStatsSupport statsSupport;

    private ChatService chatService;

    private User user;
    private AdminUser adminA;
    private AdminUser adminB;
    private ChatConversation openConversation;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                conversationRepository,
                messageRepository,
                userRepository,
                adminUserRepository,
                realtimePublisher,
                presenceService,
                offlineNotifyService,
                statsSupport);

        lenient().when(statsSupport.loadContext(any())).thenReturn(
                new ChatConversationStatsSupport.StatsContext(Map.of(), Map.of()));
        lenient().when(statsSupport.build(any(), anyBoolean(), any())).thenReturn(
                ChatConversationStatsDTO.builder().build());

        user = User.builder()
                .id(USER_ID)
                .email("agent@example.com")
                .password("x")
                .fullName("Agent User")
                .isAgency(true)
                .build();

        adminA = AdminUser.builder()
                .id(ADMIN_A)
                .email("admin-a@example.com")
                .password("x")
                .fullName("Admin A")
                .isVerified(true)
                .isActive(true)
                .build();

        adminB = AdminUser.builder()
                .id(ADMIN_B)
                .email("admin-b@example.com")
                .password("x")
                .fullName("Admin B")
                .isVerified(true)
                .isActive(true)
                .build();

        openConversation = ChatConversation.builder()
                .id(CONVERSATION_ID)
                .user(user)
                .status(ChatConversationStatus.OPEN)
                .subject("Live chat")
                .messages(new ArrayList<>())
                .build();
    }

    @Test
    void claim_firstAdminWins_secondGetsConflict() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(openConversation));
        when(adminUserRepository.findById(ADMIN_A)).thenReturn(Optional.of(adminA));
        when(adminUserRepository.findById(ADMIN_B)).thenReturn(Optional.of(adminB));
        when(conversationRepository.claimIfOpen(
                eq(CONVERSATION_ID), eq(ADMIN_A),
                eq(ChatConversationStatus.ACTIVE), eq(ChatConversationStatus.OPEN)))
                .thenReturn(1);
        when(conversationRepository.claimIfOpen(
                eq(CONVERSATION_ID), eq(ADMIN_B),
                eq(ChatConversationStatus.ACTIVE), eq(ChatConversationStatus.OPEN)))
                .thenReturn(0);
        when(conversationRepository.countUnread(anyLong(), any())).thenReturn(0L);

        ChatConversation claimed = ChatConversation.builder()
                .id(CONVERSATION_ID)
                .user(user)
                .status(ChatConversationStatus.ACTIVE)
                .assignedAdminId(ADMIN_A)
                .subject("Live chat")
                .messages(new ArrayList<>())
                .build();

        when(conversationRepository.findById(CONVERSATION_ID))
                .thenReturn(Optional.of(openConversation))
                .thenReturn(Optional.of(claimed))
                .thenReturn(Optional.of(claimed));

        ChatConversationDTO result = chatService.claim(CONVERSATION_ID, ADMIN_A);
        assertThat(result.getStatus()).isEqualTo(ChatConversationStatus.ACTIVE);
        assertThat(result.getAssignedAdminId()).isEqualTo(ADMIN_A);

        assertThatThrownBy(() -> chatService.claim(CONVERSATION_ID, ADMIN_B))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DATA_CONFLICT);

        verify(realtimePublisher, atLeastOnce()).broadcastInbox(any(ChatRealtimeEvent.class));
        verify(realtimePublisher).sendToUser(eq("agent@example.com"), any(ChatRealtimeEvent.class));
    }

    @Test
    void userCannotAccessAnotherUsersConversation() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(openConversation));

        assertThatThrownBy(() -> chatService.getForUser(CONVERSATION_ID, 999L, false))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void sendAdminMessage_requiresActiveConversation() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(openConversation));

        assertThatThrownBy(() -> chatService.sendAdminMessage(
                CONVERSATION_ID, ADMIN_A, SendChatMessageRequest.builder().body("hello").build()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATE);
    }

    @Test
    void sendUserMessage_persistsAndPublishes() {
        ChatConversation active = ChatConversation.builder()
                .id(CONVERSATION_ID)
                .user(user)
                .status(ChatConversationStatus.ACTIVE)
                .assignedAdminId(ADMIN_A)
                .subject("Live chat")
                .messages(new ArrayList<>())
                .build();

        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(active));
        when(conversationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(adminUserRepository.findById(ADMIN_A)).thenReturn(Optional.of(adminA));

        var dto = chatService.sendUserMessage(
                CONVERSATION_ID, USER_ID, SendChatMessageRequest.builder().body("Need help").build());

        assertThat(dto.getBody()).isEqualTo("Need help");
        assertThat(dto.getSenderType()).isEqualTo(ChatSenderType.USER);
        assertThat(dto.getIsInternal()).isFalse();

        ArgumentCaptor<ChatRealtimeEvent> eventCaptor = ArgumentCaptor.forClass(ChatRealtimeEvent.class);
        verify(realtimePublisher).broadcastInbox(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getType()).isEqualTo(ChatRealtimeEvent.EventType.MESSAGE);
        verify(realtimePublisher).sendToUser(eq("admin-a@example.com"), any(ChatRealtimeEvent.class));
        verify(conversationRepository).saveAndFlush(active);
        assertThat(active.getMessages()).hasSize(1);
    }

    @Test
    void sendWithAttachmentsOnly_allowed() {
        ChatConversation active = ChatConversation.builder()
                .id(CONVERSATION_ID)
                .user(user)
                .status(ChatConversationStatus.ACTIVE)
                .assignedAdminId(ADMIN_A)
                .subject("Live chat")
                .messages(new ArrayList<>())
                .build();

        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(active));
        when(conversationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(adminUserRepository.findById(ADMIN_A)).thenReturn(Optional.of(adminA));

        var dto = chatService.sendUserMessage(
                CONVERSATION_ID,
                USER_ID,
                SendChatMessageRequest.builder()
                        .attachments(List.of("https://cdn.example.com/a.png"))
                        .build());

        assertThat(dto.getBody()).isEmpty();
        assertThat(dto.getAttachments()).containsExactly("https://cdn.example.com/a.png");
        assertThat(active.getMessages().get(0).getAttachments()).isEqualTo("https://cdn.example.com/a.png");
    }

    @Test
    void sendRejectsNonImageAttachment() {
        ChatConversation active = ChatConversation.builder()
                .id(CONVERSATION_ID)
                .user(user)
                .status(ChatConversationStatus.ACTIVE)
                .assignedAdminId(ADMIN_A)
                .subject("Live chat")
                .messages(new ArrayList<>())
                .build();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> chatService.sendUserMessage(
                CONVERSATION_ID,
                USER_ID,
                SendChatMessageRequest.builder()
                        .attachments(List.of("https://cdn.example.com/a.pdf"))
                        .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("JPG/JPEG/PNG");
    }

    @Test
    void sendVoiceAttachment_allowed() {
        ChatConversation active = ChatConversation.builder()
                .id(CONVERSATION_ID)
                .user(user)
                .status(ChatConversationStatus.ACTIVE)
                .assignedAdminId(ADMIN_A)
                .subject("Live chat")
                .messages(new ArrayList<>())
                .build();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(active));
        when(conversationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(adminUserRepository.findById(ADMIN_A)).thenReturn(Optional.of(adminA));

        var dto = chatService.sendUserMessage(
                CONVERSATION_ID,
                USER_ID,
                SendChatMessageRequest.builder()
                        .attachments(List.of("https://cdn.example.com/note.webm"))
                        .build());

        assertThat(dto.getAttachments()).containsExactly("https://cdn.example.com/note.webm");
        assertThat(dto.getMedia()).hasSize(1);
        assertThat(dto.getMedia().get(0).getKind()).isEqualTo(ChatAttachmentDTO.Kind.VOICE);
    }

    @Test
    void adminCanSendAttachments() {
        ChatConversation active = ChatConversation.builder()
                .id(CONVERSATION_ID)
                .user(user)
                .status(ChatConversationStatus.ACTIVE)
                .assignedAdminId(ADMIN_A)
                .subject("Live chat")
                .messages(new ArrayList<>())
                .build();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(active));
        when(conversationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(adminUserRepository.findById(ADMIN_A)).thenReturn(Optional.of(adminA));

        var dto = chatService.sendAdminMessage(
                CONVERSATION_ID,
                ADMIN_A,
                SendChatMessageRequest.builder()
                        .attachments(List.of("https://cdn.example.com/a.png"))
                        .build());

        assertThat(dto.getAttachments()).containsExactly("https://cdn.example.com/a.png");
        assertThat(dto.getMedia()).hasSize(1);
        assertThat(dto.getMedia().get(0).getKind()).isEqualTo(ChatAttachmentDTO.Kind.IMAGE);
    }

    @Test
    void nonAgencyUserCannotSendAttachments() {
        User nonAgency = User.builder()
                .id(USER_ID)
                .email("user@example.com")
                .password("x")
                .fullName("Regular User")
                .isAgency(false)
                .build();
        ChatConversation active = ChatConversation.builder()
                .id(CONVERSATION_ID)
                .user(nonAgency)
                .status(ChatConversationStatus.ACTIVE)
                .assignedAdminId(ADMIN_A)
                .subject("Live chat")
                .messages(new ArrayList<>())
                .build();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> chatService.sendUserMessage(
                CONVERSATION_ID,
                USER_ID,
                SendChatMessageRequest.builder()
                        .attachments(List.of("https://cdn.example.com/a.png"))
                        .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only agency users");
    }

    @Test
    void internalNote_notPushedToUser_andAllowedOnOpen() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(openConversation));
        when(adminUserRepository.findById(ADMIN_A)).thenReturn(Optional.of(adminA));
        when(conversationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = chatService.sendAdminMessage(
                CONVERSATION_ID,
                ADMIN_A,
                SendChatMessageRequest.builder()
                        .body("Call this agency back")
                        .isInternal(true)
                        .build());

        assertThat(dto.getIsInternal()).isTrue();
        ArgumentCaptor<ChatRealtimeEvent> eventCaptor = ArgumentCaptor.forClass(ChatRealtimeEvent.class);
        verify(realtimePublisher).broadcastInbox(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getType()).isEqualTo(ChatRealtimeEvent.EventType.INTERNAL_NOTE);
        verify(realtimePublisher, never()).sendToUser(eq("agent@example.com"), any());
        verify(offlineNotifyService, never()).notifyUserOffline(anyLong(), anyLong(), anyString(), anyString());
    }

    @Test
    void userCannotCreateInternalNote() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(openConversation));

        assertThatThrownBy(() -> chatService.sendUserMessage(
                CONVERSATION_ID,
                USER_ID,
                SendChatMessageRequest.builder().body("secret").isInternal(true).build()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void getForUser_hidesInternalNotes() {
        ChatMessage publicMsg = ChatMessage.builder()
                .id(1L)
                .senderType(ChatSenderType.ADMIN)
                .senderId(ADMIN_A)
                .body("Hello")
                .isInternal(false)
                .isRead(false)
                .build();
        ChatMessage note = ChatMessage.builder()
                .id(2L)
                .senderType(ChatSenderType.ADMIN)
                .senderId(ADMIN_A)
                .body("Secret")
                .isInternal(true)
                .isRead(false)
                .build();
        openConversation.setStatus(ChatConversationStatus.ACTIVE);
        openConversation.setAssignedAdminId(ADMIN_A);
        openConversation.getMessages().add(publicMsg);
        openConversation.getMessages().add(note);
        publicMsg.setConversation(openConversation);
        note.setConversation(openConversation);

        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(openConversation));
        when(conversationRepository.countUnreadPublic(anyLong(), any())).thenReturn(1L);
        when(adminUserRepository.findById(ADMIN_A)).thenReturn(Optional.of(adminA));

        ChatConversationDTO dto = chatService.getForUser(CONVERSATION_ID, USER_ID, true);

        assertThat(dto.getMessages()).hasSize(1);
        assertThat(dto.getMessages().get(0).getBody()).isEqualTo("Hello");
        assertThat(dto.getLastMessagePreview()).isEqualTo("Hello");
    }

    @Test
    void closeBlocksFurtherSends() {
        ChatConversation closed = ChatConversation.builder()
                .id(CONVERSATION_ID)
                .user(user)
                .status(ChatConversationStatus.CLOSED)
                .subject("Live chat")
                .messages(new ArrayList<>())
                .build();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> chatService.sendUserMessage(
                CONVERSATION_ID, USER_ID, SendChatMessageRequest.builder().body("hi").build()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATE);
    }

    @Test
    void createOrGet_returnsExistingOpenConversation() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(conversationRepository.findFirstByUser_IdAndStatusInOrderByCreatedAtDesc(eq(USER_ID), any()))
                .thenReturn(Optional.of(openConversation));
        when(conversationRepository.countUnreadPublic(anyLong(), any())).thenReturn(0L);

        ChatConversationDTO dto = chatService.createOrGetOpenConversation(
                USER_ID, CreateChatConversationRequest.builder().subject("X").build());

        assertThat(dto.getId()).isEqualTo(CONVERSATION_ID);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void createOrGet_createsWhenNoneOpen() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(conversationRepository.findFirstByUser_IdAndStatusInOrderByCreatedAtDesc(eq(USER_ID), any()))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any())).thenAnswer(inv -> {
            ChatConversation c = inv.getArgument(0);
            c.setId(55L);
            return c;
        });
        when(conversationRepository.countUnreadPublic(anyLong(), any())).thenReturn(0L);

        ChatConversationDTO dto = chatService.createOrGetOpenConversation(
                USER_ID,
                CreateChatConversationRequest.builder()
                        .subject("Help")
                        .initialMessage("Hello")
                        .build());

        assertThat(dto.getId()).isEqualTo(55L);
        assertThat(dto.getStatus()).isEqualTo(ChatConversationStatus.OPEN);
        verify(realtimePublisher).broadcastInbox(argThat(e ->
                e.getType() == ChatRealtimeEvent.EventType.CONVERSATION_CREATED));
    }

    @Test
    void offlinePeerGetsNotification() {
        ChatConversation active = ChatConversation.builder()
                .id(CONVERSATION_ID)
                .user(user)
                .status(ChatConversationStatus.ACTIVE)
                .assignedAdminId(ADMIN_A)
                .subject("Live chat")
                .messages(new ArrayList<>())
                .build();

        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(active));
        when(adminUserRepository.findById(ADMIN_A)).thenReturn(Optional.of(adminA));
        when(conversationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        chatService.sendAdminMessage(
                CONVERSATION_ID, ADMIN_A, SendChatMessageRequest.builder().body("We can help").build());

        verify(offlineNotifyService).notifyUserOffline(
                eq(USER_ID), eq(CONVERSATION_ID), anyString(), anyString());
    }

    @Test
    void adminStartChat_createsActiveAssignedToAdmin() {
        when(adminUserRepository.findById(ADMIN_A)).thenReturn(Optional.of(adminA));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(conversationRepository.findFirstByUser_IdAndStatusInOrderByCreatedAtDesc(eq(USER_ID), any()))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any())).thenAnswer(inv -> {
            ChatConversation c = inv.getArgument(0);
            c.setId(77L);
            return c;
        });
        when(conversationRepository.findById(77L)).thenAnswer(inv -> {
            ChatConversation c = ChatConversation.builder()
                    .id(77L)
                    .user(user)
                    .status(ChatConversationStatus.ACTIVE)
                    .assignedAdminId(ADMIN_A)
                    .subject("Follow-up")
                    .messages(new ArrayList<>())
                    .build();
            return Optional.of(c);
        });
        when(conversationRepository.countUnread(anyLong(), any())).thenReturn(0L);
        when(presenceService.isOnline("user", USER_ID)).thenReturn(true);

        ChatConversationDTO dto = chatService.startConversationAsAdmin(
                ADMIN_A,
                AdminStartChatRequest.builder()
                        .userId(USER_ID)
                        .subject("Follow-up")
                        .build());

        assertThat(dto.getStatus()).isEqualTo(ChatConversationStatus.ACTIVE);
        assertThat(dto.getAssignedAdminId()).isEqualTo(ADMIN_A);
        verify(realtimePublisher).broadcastInbox(argThat(e ->
                e.getType() == ChatRealtimeEvent.EventType.CONVERSATION_CREATED));
        verify(realtimePublisher).sendToUser(eq("agent@example.com"), any());
    }

    @Test
    void adminStartChat_reusesOpenAndClaims() {
        when(adminUserRepository.findById(ADMIN_A)).thenReturn(Optional.of(adminA));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(conversationRepository.findFirstByUser_IdAndStatusInOrderByCreatedAtDesc(eq(USER_ID), any()))
                .thenReturn(Optional.of(openConversation));
        when(conversationRepository.claimIfOpen(
                eq(CONVERSATION_ID), eq(ADMIN_A),
                eq(ChatConversationStatus.ACTIVE), eq(ChatConversationStatus.OPEN)))
                .thenReturn(1);

        ChatConversation claimed = ChatConversation.builder()
                .id(CONVERSATION_ID)
                .user(user)
                .status(ChatConversationStatus.ACTIVE)
                .assignedAdminId(ADMIN_A)
                .subject("Live chat")
                .messages(new ArrayList<>())
                .build();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(claimed));
        when(conversationRepository.countUnread(anyLong(), any())).thenReturn(0L);

        ChatConversationDTO dto = chatService.startConversationAsAdmin(
                ADMIN_A,
                AdminStartChatRequest.builder().userId(USER_ID).build());

        assertThat(dto.getStatus()).isEqualTo(ChatConversationStatus.ACTIVE);
        assertThat(dto.getAssignedAdminId()).isEqualTo(ADMIN_A);
        verify(conversationRepository).claimIfOpen(
                eq(CONVERSATION_ID), eq(ADMIN_A),
                eq(ChatConversationStatus.ACTIVE), eq(ChatConversationStatus.OPEN));
    }
}
