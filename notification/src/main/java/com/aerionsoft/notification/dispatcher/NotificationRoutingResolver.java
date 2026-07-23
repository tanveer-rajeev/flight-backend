package com.aerionsoft.notification.dispatcher;

import com.aerionsoft.notification.channel.NotificationChannel;
import com.aerionsoft.notification.entity.Notification;
import com.aerionsoft.notification.entity.NotificationPreference;
import com.aerionsoft.notification.enums.NotificationChannelType;
import com.aerionsoft.notification.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
public class NotificationRoutingResolver {

    private final NotificationPreferenceRepository preferenceRepository;
    private final Set<NotificationChannelType> defaultChannels;

    public NotificationRoutingResolver(NotificationPreferenceRepository preferenceRepository,
                                       List<NotificationChannel> channels) {
        this.preferenceRepository = preferenceRepository;
        this.defaultChannels = channels.stream()
                .filter(NotificationChannel::isDefaultEnabled)
                .map(NotificationChannel::getType)
                .collect(() -> EnumSet.noneOf(NotificationChannelType.class), Set::add, Set::addAll);
    }

    public Set<NotificationChannelType> resolve(Notification notification) {
        List<NotificationPreference> preferences =
                preferenceRepository.findByUserId(notification.getUserId());

        if (preferences.isEmpty()) {
            return defaultChannels;
        }

        Set<NotificationChannelType> resolved = EnumSet.copyOf(defaultChannels);

        for (NotificationPreference preference : preferences) {
            if (!preference.getTypeCode().equals(notification.getTypeCode())) {
                continue;
            }
            if (preference.isEnabled()) {
                resolved.add(preference.getChannel());
            } else {
                resolved.remove(preference.getChannel());
            }
        }

        return resolved;
    }
}
