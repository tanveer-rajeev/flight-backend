package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.chat.AdminStartChatRequest;
import com.aerionsoft.application.dto.chat.ChatConversationDTO;
import com.aerionsoft.application.dto.chat.ChatMessageDTO;
import com.aerionsoft.application.dto.chat.ChatUserSearchItemDTO;
import com.aerionsoft.application.dto.chat.SendChatMessageRequest;
import com.aerionsoft.application.dto.common.PaginationResponseDto;
import com.aerionsoft.application.enums.chat.ChatConversationStatus;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.service.chat.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/admin/chat")
@RequiredArgsConstructor
@Slf4j
// Shared inbox: any authenticated admin panel user (provider=admin), matching WebSocket subscribe rules.
@PreAuthorize("@permissionService.isAdminUser(authentication)")
public class AdminChatController extends BaseController {

    private final ChatService chatService;

    @GetMapping("/users")
    public ResponseEntity<BaseResponse<PaginationResponseDto<ChatUserSearchItemDTO>>> searchUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long businessId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(BaseResponse.ok(
                chatService.searchUsersForChat(query, businessId, page, size)));
    }

    @PostMapping("/conversations")
    public ResponseEntity<BaseResponse<ChatConversationDTO>> startConversation(
            @Valid @RequestBody AdminStartChatRequest request) {
        Long adminId = requireAdminId();
        ChatConversationDTO dto = chatService.startConversationAsAdmin(adminId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Conversation ready", dto));
    }

    @GetMapping("/inbox")
    public ResponseEntity<BaseResponse<PaginationResponseDto<ChatConversationDTO>>> inbox(
            @RequestParam(required = false) ChatConversationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(BaseResponse.ok(chatService.listInbox(status, page, size)));
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<BaseResponse<ChatConversationDTO>> get(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean includeMessages) {
        Long adminId = requireAdminId();
        ChatConversationDTO dto = chatService.getForAdmin(id, includeMessages);
        chatService.markReadByAdmin(id, adminId);
        return ResponseEntity.ok(BaseResponse.ok(dto));
    }

    @PostMapping("/conversations/{id}/claim")
    public ResponseEntity<BaseResponse<ChatConversationDTO>> claim(@PathVariable Long id) {
        Long adminId = requireAdminId();
        return ResponseEntity.ok(BaseResponse.ok("Conversation claimed", chatService.claim(id, adminId)));
    }

    @PostMapping("/conversations/{id}/release")
    public ResponseEntity<BaseResponse<ChatConversationDTO>> release(
            @PathVariable Long id,
            Authentication authentication) {
        Long adminId = requireAdminId();
        boolean force = hasRole(authentication, "ROLE_super_admin");
        return ResponseEntity.ok(BaseResponse.ok("Conversation released",
                chatService.release(id, adminId, force)));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<BaseResponse<PaginationResponseDto<ChatMessageDTO>>> messages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(BaseResponse.ok(chatService.listMessagesForAdmin(id, page, size)));
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<BaseResponse<ChatMessageDTO>> send(
            @PathVariable Long id,
            @Valid @RequestBody SendChatMessageRequest request) {
        Long adminId = requireAdminId();
        ChatMessageDTO message = chatService.sendAdminMessage(id, adminId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Message sent", message));
    }

    @PostMapping("/conversations/{id}/read")
    public ResponseEntity<BaseResponse<Map<String, Integer>>> markRead(@PathVariable Long id) {
        Long adminId = requireAdminId();
        int updated = chatService.markReadByAdmin(id, adminId);
        return ResponseEntity.ok(BaseResponse.ok(Map.of("marked", updated)));
    }

    @PostMapping("/conversations/{id}/close")
    public ResponseEntity<BaseResponse<ChatConversationDTO>> close(@PathVariable Long id) {
        Long adminId = requireAdminId();
        return ResponseEntity.ok(BaseResponse.ok("Conversation closed", chatService.closeByAdmin(id, adminId)));
    }

    private Long requireAdminId() {
        Long adminId = getUserIdFromAuthentication();
        if (adminId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return adminId;
    }

    private static boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (role.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
