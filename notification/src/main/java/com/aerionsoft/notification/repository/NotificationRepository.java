package com.aerionsoft.notification.repository;

import com.aerionsoft.notification.entity.Notification;
import com.aerionsoft.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByBusinessId(Long id);

    Optional<Notification> findByUserId(Long id);

    List<Notification> findByUserIdAndReadFlagFalse(Long id);

    Long countByUserIdAndReadFlagFalse(Long id);

    List<Notification> findByUserIdAndType(Long rserId, NotificationType type);

    List<Notification> findTop5ByUserIdOrderByCreatedAtDesc(Long recipientUserId);

    List<Notification> findByUserIdAndTypeCode(Long userId, String typeCode);
}
