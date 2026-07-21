package com.aerionsoft.application.service.chat;

import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.enums.notification.NotificationPriority;
import com.aerionsoft.application.enums.notification.NotificationType;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.service.notification.NotificationHelper;
import com.aerionsoft.application.service.user.ActiveUserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Offline / fan-out notifications for live chat. Runs off the send hot-path
 * so high TPS messaging is not blocked by DB notification writes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatOfflineNotifyService {

    private final NotificationHelper notificationHelper;
    private final ActiveUserPresenceService presenceService;
    private final AdminUserRepository adminUserRepository;

    @Async("chatNotificationExecutor")
    public void notifyUserOffline(Long userId, Long conversationId, String adminDisplayName, String preview) {
        try {
            if (presenceService.isOnline("user", userId)) {
                return;
            }
            notificationHelper.sendCustomNotification(
                    userId,
                    NotificationType.GENERAL,
                    NotificationPriority.HIGH,
                    "New live chat message",
                    String.format("%s: %s", adminDisplayName, preview),
                    "/chat/" + conversationId,
                    "Open chat");
        } catch (Exception e) {
            log.warn("Async user chat notify failed conversation={}", conversationId, e);
        }
    }

    @Async("chatNotificationExecutor")
    public void notifyAssignedAdminOffline(Long adminId, Long conversationId, String userDisplayName, String preview) {
        try {
            if (adminId == null || presenceService.isOnline("admin", adminId)) {
                return;
            }
            notificationHelper.sendCustomNotification(
                    adminId,
                    NotificationType.GENERAL,
                    NotificationPriority.HIGH,
                    "New live chat message",
                    String.format("%s: %s", userDisplayName, preview),
                    "/admin/chat/" + conversationId,
                    "Open chat");
        } catch (Exception e) {
            log.warn("Async admin chat notify failed conversation={}", conversationId, e);
        }
    }

    @Async("chatNotificationExecutor")
    public void notifyOfflineAdminsOpenQueue(Long conversationId, String userDisplayName, String preview) {
        try {
            List<AdminUser> admins = adminUserRepository.findAdminsByRoleSlug("admin");
            if (admins == null || admins.isEmpty()) {
                return;
            }
            String title = "New live chat waiting";
            String message = String.format("%s: %s", userDisplayName, preview);
            for (AdminUser admin : admins) {
                if (admin == null || admin.getId() == null) {
                    continue;
                }
                if (presenceService.isOnline("admin", admin.getId())) {
                    continue;
                }
                notificationHelper.sendCustomNotification(
                        admin.getId(),
                        NotificationType.GENERAL,
                        NotificationPriority.HIGH,
                        title,
                        message,
                        "/admin/chat/" + conversationId,
                        "Open inbox");
            }
        } catch (Exception e) {
            log.warn("Async open-queue notify failed conversation={}", conversationId, e);
        }
    }

    @Async("chatNotificationExecutor")
    public void notifyUserChatStarted(Long userId, Long conversationId, String adminDisplayName) {
        try {
            if (presenceService.isOnline("user", userId)) {
                return;
            }
            notificationHelper.sendCustomNotification(
                    userId,
                    NotificationType.GENERAL,
                    NotificationPriority.HIGH,
                    "New live chat from support",
                    String.format("%s started a chat with you",
                            StringUtils.hasText(adminDisplayName) ? adminDisplayName : "Support"),
                    "/chat/" + conversationId,
                    "Open chat");
        } catch (Exception e) {
            log.warn("Async start-chat notify failed conversation={}", conversationId, e);
        }
    }
}
