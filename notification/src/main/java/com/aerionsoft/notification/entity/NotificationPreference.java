package com.aerionsoft.notification.entity;

import com.aerionsoft.notification.enums.NotificationChannelType;
import com.aerionsoft.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "notification_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "type", "channel"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPreference {
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannelType channel;

    @Column(nullable = false)
    private boolean enabled = true;

    public static NotificationPreference of(Long userId, NotificationType type,
                                            NotificationChannelType channel, boolean enabled) {
        NotificationPreference pref = new NotificationPreference();
        pref.userId = userId;
        pref.type = type;
        pref.channel = channel;
        pref.enabled = enabled;
        return pref;
    }
}
