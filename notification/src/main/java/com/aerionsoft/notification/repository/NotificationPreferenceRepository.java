package com.aerionsoft.notification.repository;

import com.aerionsoft.notification.entity.NotificationPreference;
import com.aerionsoft.notification.enums.NotificationChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference,Long> {

    Optional<NotificationPreference> findByUserIdAndTypeCodeAndChannel(
            Long userId, String typeCode, NotificationChannelType channel);

    List<NotificationPreference> findByUserId(Long userId);
}
