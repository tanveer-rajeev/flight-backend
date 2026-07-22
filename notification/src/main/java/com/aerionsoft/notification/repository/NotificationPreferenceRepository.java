package com.aerionsoft.notification.repository;

import com.aerionsoft.notification.dto.NotificationType;
import com.aerionsoft.notification.entity.NotificationPreference;
import com.aerionsoft.notification.enums.NotificationChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<Long,NotificationPreference> {

    NotificationPreference save(NotificationPreference preference);

    Optional<NotificationPreference> findByUserIdAndTypeAndChannel(
            Long userId, NotificationType type, NotificationChannelType channel);

    List<NotificationPreference> findByUserId(Long userId);
}
