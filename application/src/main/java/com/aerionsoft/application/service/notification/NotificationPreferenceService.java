package com.aerionsoft.application.service.notification;

import com.aerionsoft.application.dto.notification.NotificationPreferenceDTO;
import com.aerionsoft.application.entity.NotificationPreference;
import com.aerionsoft.application.repository.notification.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    /**
     * Get user notification preferences
     */
    public Optional<NotificationPreferenceDTO> getUserPreferences(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .map(this::mapToDTO);
    }

    /**
     * Get or create default preferences
     */
    public NotificationPreferenceDTO getOrCreatePreferences(Long userId) {
        Optional<NotificationPreference> existing = preferenceRepository.findByUserId(userId);

        if (existing.isPresent()) {
            return mapToDTO(existing.get());
        }

        // Create default preferences
        NotificationPreference defaultPref = NotificationPreference.builder()
                .userId(userId)
                .emailEnabled(true)
                .smsEnabled(true)
                .pushEnabled(true)
                .inAppEnabled(true)
                .quietHoursEnabled(false)
                .build();

        defaultPref = preferenceRepository.save(defaultPref);
        log.info("Created default notification preferences for user: {}", userId);

        return mapToDTO(defaultPref);
    }

    /**
     * Update user preferences
     */
    @Transactional
    public NotificationPreferenceDTO updatePreferences(Long userId, NotificationPreferenceDTO preferenceDTO) {
        log.info("Updating notification preferences for user: {}", userId);

        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElse(NotificationPreference.builder().userId(userId).build());

        preference.setEmailEnabled(preferenceDTO.getEmailEnabled());
        preference.setSmsEnabled(preferenceDTO.getSmsEnabled());
        preference.setPushEnabled(preferenceDTO.getPushEnabled());
        preference.setInAppEnabled(preferenceDTO.getInAppEnabled());
        preference.setTypePreferences(preferenceDTO.getTypePreferences());
        preference.setQuietHoursEnabled(preferenceDTO.getQuietHoursEnabled());
        preference.setQuietHoursStart(preferenceDTO.getQuietHoursStart());
        preference.setQuietHoursEnd(preferenceDTO.getQuietHoursEnd());

        preference = preferenceRepository.save(preference);
        log.info("Notification preferences updated successfully for user: {}", userId);

        return mapToDTO(preference);
    }

    /**
     * Check if user wants to receive notifications via a specific channel
     */
    public boolean isChannelEnabled(Long userId, String channel) {
        Optional<NotificationPreference> preference = preferenceRepository.findByUserId(userId);

        if (preference.isEmpty()) {
            return true; // Default to enabled if no preferences set
        }

        NotificationPreference pref = preference.get();

        return switch (channel.toUpperCase()) {
            case "EMAIL" -> Boolean.TRUE.equals(pref.getEmailEnabled());
            case "SMS" -> Boolean.TRUE.equals(pref.getSmsEnabled());
            case "PUSH" -> Boolean.TRUE.equals(pref.getPushEnabled());
            case "IN_APP" -> Boolean.TRUE.equals(pref.getInAppEnabled());
            default -> true;
        };
    }

    /**
     * Check if user is in quiet hours
     */
    public boolean isInQuietHours(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .map(NotificationPreference::isInQuietHours)
                .orElse(false);
    }

    /**
     * Map entity to DTO
     */
    private NotificationPreferenceDTO mapToDTO(NotificationPreference preference) {
        return NotificationPreferenceDTO.builder()
                .id(preference.getId())
                .userId(preference.getUserId())
                .emailEnabled(preference.getEmailEnabled())
                .smsEnabled(preference.getSmsEnabled())
                .pushEnabled(preference.getPushEnabled())
                .inAppEnabled(preference.getInAppEnabled())
                .typePreferences(preference.getTypePreferences())
                .quietHoursEnabled(preference.getQuietHoursEnabled())
                .quietHoursStart(preference.getQuietHoursStart())
                .quietHoursEnd(preference.getQuietHoursEnd())
                .build();
    }
}

