package com.aerionsoft.application.service.chat;

import com.aerionsoft.application.dto.chat.*;
import com.aerionsoft.application.dto.common.PaginationResponseDto;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.entity.chat.ChatConversation;
import com.aerionsoft.application.entity.chat.ChatMessage;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.enums.chat.ChatConversationStatus;
import com.aerionsoft.application.enums.chat.ChatSenderType;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.repository.chat.ChatConversationRepository;
import com.aerionsoft.application.repository.chat.ChatMessageRepository;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.service.user.ActiveUserPresenceService;
import com.aerionsoft.application.util.UserDateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private static final int MAX_BODY_LENGTH = 4000;
    private static final EnumSet<ChatConversationStatus> OPEN_OR_ACTIVE =
            EnumSet.of(ChatConversationStatus.OPEN, ChatConversationStatus.ACTIVE);

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final ChatRealtimePublisher realtimePublisher;
    private final ActiveUserPresenceService presenceService;
    private final ChatOfflineNotifyService offlineNotifyService;
    private final ChatConversationStatsSupport statsSupport;

    @Transactional
    public ChatConversationDTO createOrGetOpenConversation(Long userId, CreateChatConversationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return conversationRepository
                .findFirstByUser_IdAndStatusInOrderByCreatedAtDesc(userId, OPEN_OR_ACTIVE)
                .map(existing -> toConversationDto(existing, ChatSenderType.ADMIN, false, false))
                .orElseGet(() -> startNewConversation(user, request));
    }

    private ChatConversationDTO startNewConversation(User user, CreateChatConversationRequest request) {
        ChatConversation conversation = ChatConversation.builder()
                .user(user)
                .status(ChatConversationStatus.OPEN)
                .subject(StringUtils.hasText(request != null ? request.getSubject() : null)
                        ? request.getSubject().trim()
                        : "Live chat")
                .messages(new ArrayList<>())
                .build();

        if (request != null && StringUtils.hasText(request.getInitialMessage())) {
            String body = requireNonBlankBody(request.getInitialMessage());
            ChatMessage initial = ChatMessage.builder()
                    .senderType(ChatSenderType.USER)
                    .senderId(user.getId())
                    .senderName(displayName(user.getFullName(), user.getEmail()))
                    .body(body)
                    .isInternal(false)
                    .isRead(false)
                    .build();
            conversation.addMessage(initial);
        }

        ChatConversation saved = conversationRepository.save(conversation);
        ChatConversationDTO dto = toConversationDto(saved, ChatSenderType.ADMIN, true, false);

        ChatRealtimeEvent createdEvent = ChatRealtimeEvent.builder()
                .type(ChatRealtimeEvent.EventType.CONVERSATION_CREATED)
                .conversationId(saved.getId())
                .status(saved.getStatus())
                .conversation(dto)
                .build();
        realtimePublisher.broadcastInbox(createdEvent);

        log.info("Created live chat conversation {} for user {}", saved.getId(), user.getId());
        return dto;
    }

    /**
     * Admin starts (or reuses) a chat with a client user.
     * Existing OPEN/ACTIVE is returned; OPEN is auto-claimed by this admin.
     * New chats are created as ACTIVE and assigned to the admin.
     */
    @Transactional
    public ChatConversationDTO startConversationAsAdmin(Long adminId, AdminStartChatRequest request) {
        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", adminId));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));

        var existing = conversationRepository
                .findFirstByUser_IdAndStatusInOrderByCreatedAtDesc(user.getId(), OPEN_OR_ACTIVE);

        if (existing.isPresent()) {
            ChatConversation conversation = existing.get();
            if (conversation.getStatus() == ChatConversationStatus.OPEN) {
                conversationRepository.claimIfOpen(
                        conversation.getId(), adminId,
                        ChatConversationStatus.ACTIVE, ChatConversationStatus.OPEN);
                conversation = requireConversation(conversation.getId());

                ChatConversationDTO claimedDto = toConversationDto(conversation, ChatSenderType.USER, true, true);
                ChatRealtimeEvent claimedEvent = ChatRealtimeEvent.builder()
                        .type(ChatRealtimeEvent.EventType.CONVERSATION_CLAIMED)
                        .conversationId(conversation.getId())
                        .status(ChatConversationStatus.ACTIVE)
                        .assignedAdminId(adminId)
                        .assignedAdminName(displayName(admin.getFullName(), admin.getEmail()))
                        .conversation(claimedDto)
                        .build();
                realtimePublisher.broadcastInbox(claimedEvent);
                realtimePublisher.sendToUser(user.getEmail(), claimedEvent);

                maybeSendInitialAdminMessage(conversation, admin, request.getInitialMessage());
                return toConversationDto(requireConversation(conversation.getId()), ChatSenderType.USER, true, true);
            }

            // Already ACTIVE — return as-is; optional first message still allowed
            maybeSendInitialAdminMessage(conversation, admin, request.getInitialMessage());
            return toConversationDto(requireConversation(conversation.getId()), ChatSenderType.USER, true, true);
        }

        ChatConversation conversation = ChatConversation.builder()
                .user(user)
                .status(ChatConversationStatus.ACTIVE)
                .assignedAdminId(adminId)
                .claimedAt(UserDateTimeUtil.now())
                .subject(StringUtils.hasText(request.getSubject())
                        ? request.getSubject().trim()
                        : "Live chat")
                .messages(new ArrayList<>())
                .build();

        ChatConversation saved = conversationRepository.save(conversation);
        ChatConversationDTO dto = toConversationDto(saved, ChatSenderType.USER, true, true);

        ChatRealtimeEvent createdEvent = ChatRealtimeEvent.builder()
                .type(ChatRealtimeEvent.EventType.CONVERSATION_CREATED)
                .conversationId(saved.getId())
                .status(ChatConversationStatus.ACTIVE)
                .assignedAdminId(adminId)
                .assignedAdminName(displayName(admin.getFullName(), admin.getEmail()))
                .conversation(dto)
                .build();
        realtimePublisher.broadcastInbox(createdEvent);
        realtimePublisher.sendToUser(user.getEmail(), createdEvent);

        if (!presenceService.isOnline("user", user.getId()) && !StringUtils.hasText(request.getInitialMessage())) {
            offlineNotifyService.notifyUserChatStarted(
                    user.getId(),
                    saved.getId(),
                    displayName(admin.getFullName(), admin.getEmail()));
        }

        maybeSendInitialAdminMessage(saved, admin, request.getInitialMessage());

        ChatConversation refreshed = requireConversation(saved.getId());
        log.info("Admin {} started live chat {} with user {}", adminId, refreshed.getId(), user.getId());
        return toConversationDto(refreshed, ChatSenderType.USER, true, true);
    }

    private void maybeSendInitialAdminMessage(ChatConversation conversation, AdminUser admin, String initialMessage) {
        if (!StringUtils.hasText(initialMessage)) {
            return;
        }
        sendAdminMessage(
                conversation.getId(),
                admin.getId(),
                SendChatMessageRequest.builder().body(initialMessage.trim()).isInternal(false).build());
    }

    @Transactional(readOnly = true)
    public PaginationResponseDto<ChatUserSearchItemDTO> searchUsersForChat(
            String query, Long businessId, int page, int size) {
        String q = StringUtils.hasText(query) ? query.trim() : null;
        Page<User> result = userRepository.searchForLiveChat(q, businessId, PageRequest.of(page, Math.min(size, 50)));

        List<ChatUserSearchItemDTO> content = result.getContent().stream()
                .map(u -> {
                    var open = conversationRepository
                            .findFirstByUser_IdAndStatusInOrderByCreatedAtDesc(u.getId(), OPEN_OR_ACTIVE);
                    return ChatUserSearchItemDTO.builder()
                            .userId(u.getId())
                            .fullName(u.getFullName())
                            .email(u.getEmail())
                            .phoneNumber(u.getPhoneNumber())
                            .code(u.getCode())
                            .agency(u.isAgency())
                            .businessId(u.getBusiness() != null ? u.getBusiness().getId() : null)
                            .businessName(u.getBusiness() != null ? u.getBusiness().getCompanyName() : null)
                            .hasOpenChat(open.isPresent())
                            .openConversationId(open.map(ChatConversation::getId).orElse(null))
                            .build();
                })
                .collect(Collectors.toList());

        return PaginationResponseDto.<ChatUserSearchItemDTO>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PaginationResponseDto<ChatConversationDTO> listUserConversations(Long userId, int page, int size) {
        Page<ChatConversation> result = conversationRepository
                .findByUser_IdOrderByLastMessageAtDescCreatedAtDesc(userId, PageRequest.of(page, size));
        return toPage(result, ChatSenderType.ADMIN, false);
    }

    @Transactional(readOnly = true)
    public PaginationResponseDto<ChatConversationDTO> listInbox(ChatConversationStatus status, int page, int size) {
        Page<ChatConversation> result = status == null
                ? conversationRepository.findAllByOrderByLastMessageAtDescCreatedAtDesc(PageRequest.of(page, size))
                : conversationRepository.findByStatusOrderByLastMessageAtDescCreatedAtDesc(status, PageRequest.of(page, size));
        return toPage(result, ChatSenderType.USER, true);
    }

    @Transactional(readOnly = true)
    public ChatConversationDTO getForUser(Long conversationId, Long userId, boolean includeMessages) {
        ChatConversation conversation = requireConversation(conversationId);
        assertUserOwns(conversation, userId);
        return toConversationDto(conversation, ChatSenderType.ADMIN, includeMessages, false);
    }

    @Transactional(readOnly = true)
    public ChatConversationDTO getForAdmin(Long conversationId, boolean includeMessages) {
        ChatConversation conversation = requireConversation(conversationId);
        return toConversationDto(conversation, ChatSenderType.USER, includeMessages, true);
    }

    @Transactional(readOnly = true)
    public PaginationResponseDto<ChatMessageDTO> listMessagesForUser(Long conversationId, Long userId, int page, int size) {
        ChatConversation conversation = requireConversation(conversationId);
        assertUserOwns(conversation, userId);
        return listMessages(conversationId, page, size, false);
    }

    @Transactional(readOnly = true)
    public PaginationResponseDto<ChatMessageDTO> listMessagesForAdmin(Long conversationId, int page, int size) {
        requireConversation(conversationId);
        return listMessages(conversationId, page, size, true);
    }

    @Transactional
    public ChatConversationDTO claim(Long conversationId, Long adminId) {
        requireConversation(conversationId);
        adminUserRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", adminId));

        int updated = conversationRepository.claimIfOpen(
                conversationId, adminId, ChatConversationStatus.ACTIVE, ChatConversationStatus.OPEN);
        if (updated == 0) {
            ChatConversation current = requireConversation(conversationId);
            if (current.getStatus() == ChatConversationStatus.ACTIVE
                    && Objects.equals(current.getAssignedAdminId(), adminId)) {
                return toConversationDto(current, ChatSenderType.USER, false, true);
            }
            throw new BusinessException(ErrorCode.DATA_CONFLICT,
                    "Conversation is no longer open for claim");
        }

        ChatConversation conversation = requireConversation(conversationId);
        AdminUser admin = adminUserRepository.findById(adminId).orElseThrow();
        ChatConversationDTO dto = toConversationDto(conversation, ChatSenderType.USER, false, true);

        ChatRealtimeEvent event = ChatRealtimeEvent.builder()
                .type(ChatRealtimeEvent.EventType.CONVERSATION_CLAIMED)
                .conversationId(conversationId)
                .status(ChatConversationStatus.ACTIVE)
                .assignedAdminId(adminId)
                .assignedAdminName(displayName(admin.getFullName(), admin.getEmail()))
                .conversation(dto)
                .build();

        realtimePublisher.broadcastInbox(event);
        realtimePublisher.sendToUser(conversation.getUser().getEmail(), event);

        log.info("Admin {} claimed conversation {}", adminId, conversationId);
        return dto;
    }

    @Transactional
    public ChatConversationDTO release(Long conversationId, Long adminId, boolean force) {
        ChatConversation conversation = requireConversation(conversationId);
        if (conversation.getStatus() != ChatConversationStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Only ACTIVE conversations can be released");
        }
        if (!force && !Objects.equals(conversation.getAssignedAdminId(), adminId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Only the assigned admin can release this chat");
        }

        int updated = conversationRepository.releaseIfOwned(
                conversationId,
                conversation.getAssignedAdminId(),
                ChatConversationStatus.OPEN,
                ChatConversationStatus.ACTIVE);
        if (updated == 0 && force) {
            conversation.setStatus(ChatConversationStatus.OPEN);
            conversation.setAssignedAdminId(null);
            conversationRepository.save(conversation);
        } else if (updated == 0) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "Failed to release conversation");
        }

        ChatConversation refreshed = requireConversation(conversationId);
        ChatConversationDTO dto = toConversationDto(refreshed, ChatSenderType.USER, false, true);

        ChatRealtimeEvent event = ChatRealtimeEvent.builder()
                .type(ChatRealtimeEvent.EventType.CONVERSATION_RELEASED)
                .conversationId(conversationId)
                .status(ChatConversationStatus.OPEN)
                .conversation(dto)
                .build();
        realtimePublisher.broadcastInbox(event);
        realtimePublisher.sendToUser(refreshed.getUser().getEmail(), event);

        return dto;
    }

    @Transactional
    public ChatMessageDTO sendUserMessage(Long conversationId, Long userId, SendChatMessageRequest request) {
        ChatConversation conversation = requireConversation(conversationId);
        assertUserOwns(conversation, userId);
        assertNotClosed(conversation);

        if (Boolean.TRUE.equals(request.getIsInternal())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Users cannot create internal notes");
        }

        User user = conversation.getUser() != null && Objects.equals(conversation.getUser().getId(), userId)
                ? conversation.getUser()
                : userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        ContentParts content = validateContent(request);
        assertAgencyMayAttach(user, content.attachmentsCsv());

        ChatMessage message = ChatMessage.builder()
                .senderType(ChatSenderType.USER)
                .senderId(userId)
                .senderName(displayName(user.getFullName(), user.getEmail()))
                .body(content.body())
                .attachments(content.attachmentsCsv())
                .isInternal(false)
                .isRead(false)
                .build();
        conversation.addMessage(message);
        // Flush so message.id is available for the WS payload immediately
        conversationRepository.saveAndFlush(conversation);

        ChatMessageDTO messageDto = toMessageDto(message);
        publishMessageEventsFast(conversation, messageDto, ChatSenderType.USER, false);

        String notifyPreview = notificationPreview(content);
        String userLabel = displayName(user.getFullName(), user.getEmail());
        if (conversation.getAssignedAdminId() != null) {
            offlineNotifyService.notifyAssignedAdminOffline(
                    conversation.getAssignedAdminId(), conversationId, userLabel, notifyPreview);
        } else if (conversation.getStatus() == ChatConversationStatus.OPEN) {
            offlineNotifyService.notifyOfflineAdminsOpenQueue(conversationId, userLabel, notifyPreview);
        }

        return messageDto;
    }

    @Transactional
    public ChatMessageDTO sendAdminMessage(Long conversationId, Long adminId, SendChatMessageRequest request) {
        ChatConversation conversation = requireConversation(conversationId);
        assertNotClosed(conversation);

        boolean internal = Boolean.TRUE.equals(request.getIsInternal());
        if (!internal && conversation.getStatus() != ChatConversationStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Claim the conversation before sending a reply");
        }

        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", adminId));
        ContentParts content = validateContent(request);

        ChatMessage message = ChatMessage.builder()
                .senderType(ChatSenderType.ADMIN)
                .senderId(adminId)
                .senderName(displayName(admin.getFullName(), admin.getEmail()))
                .body(content.body())
                .attachments(content.attachmentsCsv())
                .isInternal(internal)
                .isRead(false)
                .build();
        conversation.addMessage(message);
        conversationRepository.saveAndFlush(conversation);

        ChatMessageDTO messageDto = toMessageDto(message);
        publishMessageEventsFast(conversation, messageDto, ChatSenderType.ADMIN, internal);

        if (!internal && conversation.getUser() != null) {
            offlineNotifyService.notifyUserOffline(
                    conversation.getUser().getId(),
                    conversationId,
                    displayName(admin.getFullName(), "Support"),
                    notificationPreview(content));
        }

        return messageDto;
    }

    @Transactional
    public int markReadByUser(Long conversationId, Long userId) {
        ChatConversation conversation = requireConversation(conversationId);
        assertUserOwns(conversation, userId);
        int updated = messageRepository.markUnreadPublicAsRead(
                conversationId, ChatSenderType.ADMIN, UserDateTimeUtil.now());
        if (updated > 0) {
            publishReadEvent(conversation, ChatSenderType.USER, userId);
        }
        return updated;
    }

    @Transactional
    public int markReadByAdmin(Long conversationId, Long adminId) {
        ChatConversation conversation = requireConversation(conversationId);
        int updated = messageRepository.markUnreadAsRead(
                conversationId, ChatSenderType.USER, UserDateTimeUtil.now());
        if (updated > 0) {
            publishReadEvent(conversation, ChatSenderType.ADMIN, adminId);
        }
        return updated;
    }

    @Transactional
    public ChatConversationDTO closeByUser(Long conversationId, Long userId) {
        ChatConversation conversation = requireConversation(conversationId);
        assertUserOwns(conversation, userId);
        return close(conversation, ChatSenderType.USER, userId);
    }

    @Transactional
    public ChatConversationDTO closeByAdmin(Long conversationId, Long adminId) {
        ChatConversation conversation = requireConversation(conversationId);
        return close(conversation, ChatSenderType.ADMIN, adminId);
    }

    public void publishTyping(Long conversationId, ChatSenderType senderType, Long senderId, boolean typing) {
        ChatConversation conversation = requireConversation(conversationId);
        if (senderType == ChatSenderType.USER) {
            assertUserOwns(conversation, senderId);
        }
        assertNotClosed(conversation);

        ChatRealtimeEvent event = ChatRealtimeEvent.builder()
                .type(ChatRealtimeEvent.EventType.TYPING)
                .conversationId(conversationId)
                .typingSenderType(senderType)
                .typingSenderId(senderId)
                .typing(typing)
                .build();

        if (senderType == ChatSenderType.USER) {
            if (conversation.getAssignedAdminId() != null) {
                adminUserRepository.findById(conversation.getAssignedAdminId())
                        .ifPresent(admin -> realtimePublisher.sendToUser(admin.getEmail(), event));
            }
            realtimePublisher.broadcastInbox(event);
        } else {
            realtimePublisher.sendToUser(conversation.getUser().getEmail(), event);
        }
    }

    private ChatConversationDTO close(ChatConversation conversation, ChatSenderType closerType, Long closerId) {
        boolean adminView = closerType == ChatSenderType.ADMIN;
        if (conversation.getStatus() == ChatConversationStatus.CLOSED) {
            return toConversationDto(conversation,
                    closerType == ChatSenderType.USER ? ChatSenderType.ADMIN : ChatSenderType.USER,
                    false,
                    adminView);
        }
        conversation.setStatus(ChatConversationStatus.CLOSED);
        conversation.setClosedAt(UserDateTimeUtil.now());
        conversation.setClosedByType(closerType);
        conversation.setClosedById(closerId);
        conversationRepository.save(conversation);

        ChatConversationDTO dto = toConversationDto(conversation,
                closerType == ChatSenderType.USER ? ChatSenderType.ADMIN : ChatSenderType.USER,
                false,
                adminView);

        ChatRealtimeEvent event = ChatRealtimeEvent.builder()
                .type(ChatRealtimeEvent.EventType.CONVERSATION_CLOSED)
                .conversationId(conversation.getId())
                .status(ChatConversationStatus.CLOSED)
                .conversation(dto)
                .build();
        realtimePublisher.broadcastInbox(event);
        realtimePublisher.sendToUser(conversation.getUser().getEmail(), event);
        if (conversation.getAssignedAdminId() != null) {
            adminUserRepository.findById(conversation.getAssignedAdminId())
                    .ifPresent(admin -> realtimePublisher.sendToUser(admin.getEmail(), event));
        }

        return dto;
    }

    /**
     * Hot-path publish: no unread-count queries / heavy conversation DTO rebuild.
     * Clients already have conversation state; they only need the new message.
     */
    private void publishMessageEventsFast(ChatConversation conversation,
                                          ChatMessageDTO messageDto,
                                          ChatSenderType senderType,
                                          boolean internal) {
        User user = conversation.getUser();
        ChatConversationDTO summary = ChatConversationDTO.builder()
                .id(conversation.getId())
                .userId(user != null ? user.getId() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .userFullName(user != null ? user.getFullName() : null)
                .status(conversation.getStatus())
                .assignedAdminId(conversation.getAssignedAdminId())
                .subject(conversation.getSubject())
                .lastMessageAt(conversation.getLastMessageAt())
                .lastMessagePreview(messagePreviewFromDto(messageDto))
                .build();

        ChatRealtimeEvent event = ChatRealtimeEvent.builder()
                .type(internal ? ChatRealtimeEvent.EventType.INTERNAL_NOTE : ChatRealtimeEvent.EventType.MESSAGE)
                .conversationId(conversation.getId())
                .status(conversation.getStatus())
                .assignedAdminId(conversation.getAssignedAdminId())
                .message(messageDto)
                .conversation(summary)
                .build();

        realtimePublisher.broadcastInbox(event);

        if (conversation.getAssignedAdminId() != null) {
            adminUserRepository.findById(conversation.getAssignedAdminId())
                    .map(AdminUser::getEmail)
                    .ifPresent(email -> realtimePublisher.sendToUser(email, event));
        }

        if (!internal && senderType == ChatSenderType.ADMIN && user != null) {
            realtimePublisher.sendToUser(user.getEmail(), event);
        }
    }

    private void publishReadEvent(ChatConversation conversation, ChatSenderType readerType, Long readerId) {
        ChatRealtimeEvent event = ChatRealtimeEvent.builder()
                .type(ChatRealtimeEvent.EventType.READ)
                .conversationId(conversation.getId())
                .readByType(readerType)
                .readById(readerId)
                .build();

        if (readerType == ChatSenderType.USER) {
            if (conversation.getAssignedAdminId() != null) {
                adminUserRepository.findById(conversation.getAssignedAdminId())
                        .ifPresent(admin -> realtimePublisher.sendToUser(admin.getEmail(), event));
            }
            realtimePublisher.broadcastInbox(event);
        } else {
            realtimePublisher.sendToUser(conversation.getUser().getEmail(), event);
        }
    }

    private PaginationResponseDto<ChatMessageDTO> listMessages(Long conversationId, int page, int size,
                                                               boolean includeInternal) {
        Page<ChatMessage> result = includeInternal
                ? messageRepository.findByConversation_IdOrderByIdDesc(conversationId, PageRequest.of(page, size))
                : messageRepository.findByConversation_IdAndIsInternalFalseOrderByIdDesc(
                conversationId, PageRequest.of(page, size));
        List<ChatMessageDTO> content = result.getContent().stream()
                .map(this::toMessageDto)
                .collect(Collectors.toList());
        return PaginationResponseDto.<ChatMessageDTO>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    private PaginationResponseDto<ChatConversationDTO> toPage(Page<ChatConversation> page,
                                                              ChatSenderType unreadFrom,
                                                              boolean adminView) {
        List<Long> ids = page.getContent().stream().map(ChatConversation::getId).collect(Collectors.toList());
        ChatConversationStatsSupport.StatsContext statsContext = statsSupport.loadContext(ids);
        List<ChatConversationDTO> content = page.getContent().stream()
                .map(c -> toConversationDto(c, unreadFrom, false, adminView, statsContext))
                .collect(Collectors.toList());
        return PaginationResponseDto.<ChatConversationDTO>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private ChatConversationDTO toConversationDto(ChatConversation conversation,
                                                   ChatSenderType unreadFrom,
                                                   boolean includeMessages,
                                                   boolean adminView) {
        return toConversationDto(conversation, unreadFrom, includeMessages, adminView, null);
    }

    private ChatConversationDTO toConversationDto(ChatConversation conversation,
                                                   ChatSenderType unreadFrom,
                                                   boolean includeMessages,
                                                   boolean adminView,
                                                   ChatConversationStatsSupport.StatsContext statsContext) {
        User user = conversation.getUser();
        String adminName = null;
        String adminEmail = null;
        if (conversation.getAssignedAdminId() != null) {
            var adminOpt = adminUserRepository.findById(conversation.getAssignedAdminId());
            adminName = adminOpt.map(a -> displayName(a.getFullName(), a.getEmail())).orElse(null);
            adminEmail = adminOpt.map(AdminUser::getEmail).orElse(null);
        }

        long unread = adminView
                ? conversationRepository.countUnread(conversation.getId(), unreadFrom)
                : conversationRepository.countUnreadPublic(conversation.getId(), unreadFrom);

        List<ChatMessage> loaded = conversation.getMessages();
        String lastPreview = null;
        if (loaded != null && !loaded.isEmpty()) {
            for (int i = loaded.size() - 1; i >= 0; i--) {
                ChatMessage m = loaded.get(i);
                if (!adminView && Boolean.TRUE.equals(m.getIsInternal())) {
                    continue;
                }
                lastPreview = messagePreview(m);
                break;
            }
        }

        ChatConversationDTO.ChatConversationDTOBuilder builder = ChatConversationDTO.builder()
                .id(conversation.getId())
                .userId(user != null ? user.getId() : null)
                .userFullName(user != null ? user.getFullName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .businessId(user != null && user.getBusiness() != null ? user.getBusiness().getId() : null)
                .userBusinessName(user != null && user.getBusiness() != null ? user.getBusiness().getCompanyName() : null)
                .assignedAdminId(conversation.getAssignedAdminId())
                .assignedAdminName(adminName)
                .assignedAdminEmail(adminEmail)
                .status(conversation.getStatus())
                .subject(conversation.getSubject())
                .lastMessageAt(conversation.getLastMessageAt())
                .lastMessagePreview(lastPreview)
                .unreadCount(unread)
                .closedAt(conversation.getClosedAt())
                .closedByType(conversation.getClosedByType())
                .closedById(conversation.getClosedById())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .stats(statsSupport.build(conversation, adminView, statsContext));

        if (includeMessages) {
            List<ChatMessage> messages = loaded;
            if (messages == null || messages.isEmpty()) {
                messages = adminView
                        ? messageRepository.findByConversation_IdOrderByIdAsc(conversation.getId())
                        : messageRepository.findByConversation_IdAndIsInternalFalseOrderByIdAsc(conversation.getId());
            } else if (!adminView) {
                messages = messages.stream()
                        .filter(m -> !Boolean.TRUE.equals(m.getIsInternal()))
                        .collect(Collectors.toList());
            }
            builder.messages(messages.stream().map(this::toMessageDto).collect(Collectors.toList()));
            if (lastPreview == null && !messages.isEmpty()) {
                builder.lastMessagePreview(messagePreview(messages.get(messages.size() - 1)));
            }
        }

        return builder.build();
    }

    private ChatMessageDTO toMessageDto(ChatMessage message) {
        List<String> urls = parseAttachments(message.getAttachments());
        return ChatMessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversation() != null ? message.getConversation().getId() : null)
                .senderType(message.getSenderType())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .body(message.getBody())
                .attachments(urls)
                .media(toMedia(urls))
                .isInternal(Boolean.TRUE.equals(message.getIsInternal()))
                .isRead(message.getIsRead())
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt())
                .createdTimeOffset(message.getCreatedTimeOffset())
                .build();
    }

    private ChatConversation requireConversation(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChatConversation", id));
    }

    private void assertUserOwns(ChatConversation conversation, Long userId) {
        if (conversation.getUser() == null || !Objects.equals(conversation.getUser().getId(), userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Access denied to this conversation");
        }
    }

    /** Image/voice attachments are agency-only (mother agency or staff under one). */
    private void assertAgencyMayAttach(User user, String attachmentsCsv) {
        if (!StringUtils.hasText(attachmentsCsv)) {
            return;
        }
        User root = user.getParentUser() != null ? user.getParentUser() : user;
        if (!root.isAgency()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Only agency users can send images or voice notes");
        }
    }

    private void assertNotClosed(ChatConversation conversation) {
        if (conversation.getStatus() == ChatConversationStatus.CLOSED) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Cannot message a closed conversation. Start a new chat.");
        }
    }

    private ContentParts validateContent(SendChatMessageRequest request) {
        if (request == null) {
            throw BusinessException.validation("Message body or attachments are required", null);
        }
        List<String> attachments = normalizeAttachments(request.getAttachments());
        String body = request.getBody() != null ? request.getBody().trim() : "";
        if (body.length() > MAX_BODY_LENGTH) {
            throw BusinessException.validation(
                    "Message must be at most " + MAX_BODY_LENGTH + " characters", null);
        }
        if (!StringUtils.hasText(body) && attachments.isEmpty()) {
            throw BusinessException.validation("Message body or attachments are required", null);
        }
        return new ContentParts(StringUtils.hasText(body) ? body : "", serializeAttachments(attachments));
    }

    private String requireNonBlankBody(String body) {
        if (!StringUtils.hasText(body)) {
            throw BusinessException.validation("Message body is required", null);
        }
        String trimmed = body.trim();
        if (trimmed.length() > MAX_BODY_LENGTH) {
            throw BusinessException.validation(
                    "Message must be at most " + MAX_BODY_LENGTH + " characters", null);
        }
        return trimmed;
    }

    private static final int MAX_ATTACHMENTS = 5;
    private static final java.util.Set<String> ALLOWED_IMAGE_EXTENSIONS =
            java.util.Set.of("jpg", "jpeg", "png");
    private static final java.util.Set<String> ALLOWED_VOICE_EXTENSIONS =
            java.util.Set.of("webm", "ogg", "mp3", "m4a", "wav", "aac");

    private static List<String> normalizeAttachments(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> cleaned = raw.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (cleaned.size() > MAX_ATTACHMENTS) {
            throw BusinessException.validation("At most " + MAX_ATTACHMENTS + " attachments allowed", null);
        }
        for (String url : cleaned) {
            if (url.length() > 2048) {
                throw BusinessException.validation("Attachment URL too long", null);
            }
            if (!(url.startsWith("http://") || url.startsWith("https://"))) {
                throw BusinessException.validation("Attachment URLs must be http(s)", null);
            }
            if (classifyAttachment(url) == null) {
                throw BusinessException.validation(
                        "Chat attachments allow only JPG/JPEG/PNG images or voice notes (webm, ogg, mp3, m4a, wav, aac)",
                        null);
            }
        }
        return cleaned;
    }

    private static List<ChatAttachmentDTO> toMedia(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return List.of();
        }
        return urls.stream()
                .map(url -> ChatAttachmentDTO.builder()
                        .url(url)
                        .kind(classifyAttachment(url))
                        .build())
                .filter(a -> a.getKind() != null)
                .collect(Collectors.toList());
    }

    /** Returns IMAGE, VOICE, or null if unsupported. */
    private static ChatAttachmentDTO.Kind classifyAttachment(String url) {
        String ext = fileExtension(url);
        if (ext == null) {
            return null;
        }
        if (ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            return ChatAttachmentDTO.Kind.IMAGE;
        }
        if (ALLOWED_VOICE_EXTENSIONS.contains(ext)) {
            return ChatAttachmentDTO.Kind.VOICE;
        }
        return null;
    }

    private static String fileExtension(String url) {
        String path = url;
        int q = path.indexOf('?');
        if (q >= 0) {
            path = path.substring(0, q);
        }
        int hash = path.indexOf('#');
        if (hash >= 0) {
            path = path.substring(0, hash);
        }
        int slash = path.lastIndexOf('/');
        String file = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = file.lastIndexOf('.');
        if (dot < 0 || dot == file.length() - 1) {
            return null;
        }
        return file.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
    }

    private static String serializeAttachments(List<String> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        return String.join(",", attachments);
    }

    private static List<String> parseAttachments(String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private static String displayName(String fullName, String fallback) {
        return StringUtils.hasText(fullName) ? fullName : fallback;
    }

    private static String preview(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 100 ? body.substring(0, 100) + "..." : body;
    }

    private static String messagePreview(ChatMessage message) {
        if (StringUtils.hasText(message.getBody())) {
            return preview(message.getBody());
        }
        List<String> attachments = parseAttachments(message.getAttachments());
        if (!attachments.isEmpty()) {
            ChatAttachmentDTO.Kind kind = classifyAttachment(attachments.get(attachments.size() - 1));
            if (kind == ChatAttachmentDTO.Kind.VOICE) {
                return "[voice]";
            }
            return "[image]";
        }
        if (Boolean.TRUE.equals(message.getIsInternal())) {
            return "[internal note]";
        }
        return "";
    }

    private static String messagePreviewFromDto(ChatMessageDTO messageDto) {
        if (messageDto == null) {
            return "";
        }
        if (StringUtils.hasText(messageDto.getBody())) {
            return preview(messageDto.getBody());
        }
        if (messageDto.getAttachments() != null && !messageDto.getAttachments().isEmpty()) {
            ChatAttachmentDTO.Kind kind = classifyAttachment(
                    messageDto.getAttachments().get(messageDto.getAttachments().size() - 1));
            if (kind == ChatAttachmentDTO.Kind.VOICE) {
                return "[voice]";
            }
            return "[image]";
        }
        if (Boolean.TRUE.equals(messageDto.getIsInternal())) {
            return "[internal note]";
        }
        return "";
    }

    private static String notificationPreview(ContentParts content) {
        if (StringUtils.hasText(content.body())) {
            return preview(content.body());
        }
        return "[attachment]";
    }

    private record ContentParts(String body, String attachmentsCsv) {
    }
}
