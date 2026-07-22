package com.aerionsoft.notification.repository;

import com.aerionsoft.notification.dto.NotificationType;
import com.aerionsoft.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Long, Notification> {
    Notification save(Notification notification);

    Optional<Notification> findById(Long id);

    Optional<Notification> findByBusinessId(Long id);

    Optional<Notification> findByUserId(Long id);

    Optional<Notification> findByUserIdAndReadFlagFalse(Long id);

    Long countByUserIdAndReadFlagFalse(Long id);

    List<Notification> findByRecipientUserIdAndType(Long recipientUserId, NotificationType type);

    List<Notification> findTop5ByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);
}
