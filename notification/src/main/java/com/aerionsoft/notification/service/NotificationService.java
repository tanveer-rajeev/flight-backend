package com.aerionsoft.notification.service;

import com.aerionsoft.notification.dispatcher.NotificationDispatcher;
import com.aerionsoft.notification.dto.request.NotificationReadRequest;
import com.aerionsoft.notification.dto.request.NotificationRequest;
import com.aerionsoft.notification.dto.response.NotificationResponse;
import com.aerionsoft.notification.dto.response.NotificationSummaryResponse;
import com.aerionsoft.notification.entity.Notification;
import com.aerionsoft.notification.exception.NotificationAccessDeniedException;
import com.aerionsoft.notification.exception.NotificationNotFoundException;
import com.aerionsoft.notification.factory.NotificationFactory;
import com.aerionsoft.notification.mapper.NotificationMapper;
import com.aerionsoft.notification.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationFactory notificationFactory;
    private final NotificationDispatcher notificationDispatcher;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationFactory notificationFactory,
                               NotificationDispatcher notificationDispatcher) {
        this.notificationRepository = notificationRepository;
        this.notificationFactory = notificationFactory;
        this.notificationDispatcher = notificationDispatcher;
    }

    @Transactional
    public NotificationResponse send(NotificationRequest request) {
        Notification notification = notificationFactory.createFrom(request);
        notification = notificationRepository.save(notification);

        log.info("Notification created id={}, userId={}, typeCode={}",
                notification.getId(), notification.getUserId(), notification.getTypeCode());

        notificationDispatcher.dispatch(notification, request.recipientContacts());

        return NotificationMapper.toResponse(notification);
    }

    private Notification getUserNotification(Long userId) {
        return notificationRepository.findByUserId(userId).orElseThrow(
                () -> new NotificationNotFoundException("Notification not found for {}" + userId)
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId) {
        return Collections.singletonList(NotificationMapper.toResponse(getUserNotification(userId)));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        return NotificationMapper.toResponseList(notificationRepository.findByUserIdAndReadFlagFalse(userId));
    }

    @Transactional(readOnly = true)
    public NotificationSummaryResponse getSummary(Long userId) {
        long unreadCount = notificationRepository.countByUserIdAndReadFlagFalse(userId);
        List<Notification> recent = notificationRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId);
        return NotificationMapper.toSummaryResponse(unreadCount, recent);
    }

    @Transactional
    public void markAsRead(Long userId, NotificationReadRequest request) {
        for (Long notificationId : request.notificationIds()) {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> NotificationNotFoundException.forId(notificationId));

            assertOwnership(notification, userId);
            notification.markAsRead();
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAsArchived(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> NotificationNotFoundException.forId(notificationId));

        assertOwnership(notification, userId);
        notification.markAsArchived();
        notificationRepository.save(notification);
    }

    private void assertOwnership(Notification notification, Long userId) {
        if (!notification.getUserId().equals(userId)) {
            throw new NotificationAccessDeniedException(
                    "User " + userId + " is not permitted to modify notification " + notification.getId());
        }
    }
}
