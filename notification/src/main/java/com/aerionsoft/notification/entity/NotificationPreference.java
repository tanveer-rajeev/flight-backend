package com.aerionsoft.notification.entity;

import com.aerionsoft.notification.enums.NotificationCategory;
import com.aerionsoft.notification.enums.NotificationChannelType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "notification_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "type_code", "channel"})
)
@Getter
@Setter
@NoArgsConstructor()
public class NotificationPreference {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "type_code", nullable = false, length = 60)
    private String typeCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannelType channel;

    @Column(nullable = false)
    private boolean enabled = true;

    public static NotificationPreference of(Long userId, NotificationType type,
                                            NotificationChannelType channel, boolean enabled) {
        NotificationPreference pref = new NotificationPreference();
        pref.userId = userId;
        pref.typeCode = type.getCode();
        pref.category = type.getCategory();
        pref.channel = channel;
        pref.enabled = enabled;
        return pref;
    }
}