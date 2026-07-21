package com.aerionsoft.application.controller.client;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.chat.ChatConversationDTO;
import com.aerionsoft.application.dto.chat.ChatMessageDTO;
import com.aerionsoft.application.dto.chat.CreateChatConversationRequest;
import com.aerionsoft.application.dto.chat.SendChatMessageRequest;
import com.aerionsoft.application.dto.common.PaginationResponseDto;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.service.chat.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController extends BaseController {

    private final ChatService chatService;

    @PostMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ChatConversationDTO>> createOrGet(
            @Valid @RequestBody(required = false) CreateChatConversationRequest request) {
        Long userId = requireUserId();
        if (!"user".equalsIgnoreCase(getProviderFromSecurity())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Only client users can start live chat");
        }
        ChatConversationDTO dto = chatService.createOrGetOpenConversation(
                userId, request != null ? request : new CreateChatConversationRequest());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Conversation ready", dto));
    }

    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<PaginationResponseDto<ChatConversationDTO>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = requireUserId();
        return ResponseEntity.ok(BaseResponse.ok(chatService.listUserConversations(userId, page, size)));
    }

    @GetMapping("/conversations/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ChatConversationDTO>> get(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean includeMessages) {
        Long userId = requireUserId();
        ChatConversationDTO dto = chatService.getForUser(id, userId, includeMessages);
        chatService.markReadByUser(id, userId);
        return ResponseEntity.ok(BaseResponse.ok(dto));
    }

    @GetMapping("/conversations/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<PaginationResponseDto<ChatMessageDTO>>> messages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = requireUserId();
        return ResponseEntity.ok(BaseResponse.ok(chatService.listMessagesForUser(id, userId, page, size)));
    }

    @PostMapping("/conversations/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ChatMessageDTO>> send(
            @PathVariable Long id,
            @Valid @RequestBody SendChatMessageRequest request) {
        Long userId = requireUserId();
        ChatMessageDTO message = chatService.sendUserMessage(id, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Message sent", message));
    }

    @PostMapping("/conversations/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Map<String, Integer>>> markRead(@PathVariable Long id) {
        Long userId = requireUserId();
        int updated = chatService.markReadByUser(id, userId);
        return ResponseEntity.ok(BaseResponse.ok(Map.of("marked", updated)));
    }

    @PostMapping("/conversations/{id}/close")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ChatConversationDTO>> close(@PathVariable Long id) {
        Long userId = requireUserId();
        return ResponseEntity.ok(BaseResponse.ok("Conversation closed", chatService.closeByUser(id, userId)));
    }

    private Long requireUserId() {
        Long userId = getUserIdFromAuthentication();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    private String getProviderFromSecurity() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        return getProviderName(authentication);
    }
}
