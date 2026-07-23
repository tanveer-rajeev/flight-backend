package com.aerionsoft.notification.service;

import com.aerionsoft.notification.dto.request.NotificationPreferenceRequest;
import com.aerionsoft.notification.dto.response.NotificationPreferenceResponse;
import com.aerionsoft.notification.entity.NotificationPreference;
import com.aerionsoft.notification.exception.InvalidNotificationTypeException;
import com.aerionsoft.notification.registry.NotificationTypeRegistry;
import com.aerionsoft.notification.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationTypeRegistry typeRegistry;

    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository,
                                         NotificationTypeRegistry typeRegistry) {
        this.preferenceRepository = preferenceRepository;
        this.typeRegistry = typeRegistry;
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getPreferences(Long userId) {
        return preferenceRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NotificationPreferenceResponse upsert(Long userId, NotificationPreferenceRequest request) {
        if (!typeRegistry.isValidCode(request.typeCode())) {
            throw new InvalidNotificationTypeException(request.typeCode());
        }

        NotificationPreference preference = preferenceRepository
                .findByUserIdAndTypeCodeAndChannel(userId, request.typeCode(), request.channel())
                .orElseGet(() -> newPreference(userId, request));

        preference.setEnabled(request.enabled());

        return toResponse(preferenceRepository.save(preference));
    }

    private NotificationPreference newPreference(Long userId, NotificationPreferenceRequest request) {
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(userId);
        preference.setTypeCode(request.typeCode());
        preference.setChannel(request.channel());
        return preference;
    }

    private NotificationPreferenceResponse toResponse(NotificationPreference preference) {
        return new NotificationPreferenceResponse(
                preference.getId(),
                preference.getTypeCode(),
                preference.getChannel(),
                preference.isEnabled()
        );
    }
}