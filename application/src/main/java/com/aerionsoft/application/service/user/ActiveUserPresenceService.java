package com.aerionsoft.application.service.user;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.dto.admin.summery.ActiveUserDto;
import com.aerionsoft.application.dto.admin.summery.ActiveUsersResponse;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.websocket.ActiveUsersWebSocketBroadcaster;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class ActiveUserPresenceService {

    private static final String KEY_PREFIX = "active-user:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final ActiveUsersWebSocketBroadcaster activeUsersBroadcaster;

    @Value("${presence.ttl-seconds:90}")
    private long presenceTtlSeconds;

    public ActiveUserPresenceService(StringRedisTemplate redisTemplate,
                                     ObjectMapper objectMapper,
                                     UserRepository userRepository,
                                     AdminUserRepository adminUserRepository,
                                     @Lazy ActiveUsersWebSocketBroadcaster activeUsersBroadcaster) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.adminUserRepository = adminUserRepository;
        this.activeUsersBroadcaster = activeUsersBroadcaster;
    }

    public void markOnline(String provider, Long userId, String ipAddress, String userAgent) {
        if (provider == null || userId == null) {
            return;
        }

        Optional<ActiveUserDto> snapshot = buildSnapshot(provider, userId, ipAddress, userAgent);
        if (snapshot.isEmpty()) {
            return;
        }

        writePresence(snapshot.get());
    }

    public void markOnline(CustomUserDetails principal, String ipAddress, String userAgent) {
        if (principal == null) {
            return;
        }
        markOnline(principal.getProvider(), principal.getId(), ipAddress, userAgent);
    }

    public void touch(CustomUserDetails principal) {
        if (principal == null) {
            return;
        }

        String key = redisKey(principal.getProvider(), principal.getId());
        if (redisTemplate.hasKey(key)) {
            redisTemplate.expire(key, Duration.ofSeconds(presenceTtlSeconds));
        }
    }

    /** Refreshes TTL when already online; otherwise loads profile and marks online. */
    public void recordActivity(CustomUserDetails principal, String ipAddress, String userAgent) {
        if (principal == null) {
            return;
        }
        String key = redisKey(principal.getProvider(), principal.getId());
        if (redisTemplate.hasKey(key)) {
            touch(principal);
        } else {
            markOnline(principal, ipAddress, userAgent);
        }
    }

    public void markOffline(String provider, Long userId) {
        if (provider == null || userId == null) {
            return;
        }
        redisTemplate.delete(redisKey(provider, userId));
        activeUsersBroadcaster.publishActiveUsers();
    }

    public void markOffline(CustomUserDetails principal) {
        if (principal == null) {
            return;
        }
        markOffline(principal.getProvider(), principal.getId());
    }

    /** True when Redis still holds a live presence key for this provider/user. */
    public boolean isOnline(String provider, Long userId) {
        if (provider == null || userId == null) {
            return false;
        }
        Boolean present = redisTemplate.hasKey(redisKey(provider, userId));
        return Boolean.TRUE.equals(present);
    }

    public ActiveUsersResponse getActiveUsers() {
        List<ActiveUserDto> regularUsers = new ArrayList<>();
        List<ActiveUserDto> agencyUsers = new ArrayList<>();
        List<ActiveUserDto> adminUsers = new ArrayList<>();

        for (ActiveUserDto dto : listOnlineUsers()) {
            if ("ADMIN".equalsIgnoreCase(dto.getUserType())) {
                adminUsers.add(dto);
            } else if (dto.isAgency()) {
                agencyUsers.add(dto);
            } else {
                regularUsers.add(dto);
            }
        }

        List<ActiveUserDto> allActiveUsers = new ArrayList<>(regularUsers);
        allActiveUsers.addAll(agencyUsers);
        allActiveUsers.addAll(adminUsers);

        return ActiveUsersResponse.builder()
                .totalActiveUsers((long) allActiveUsers.size())
                .activeRegularUsers((long) regularUsers.size())
                .activeAgencyUsers((long) agencyUsers.size())
                .activeAdminUsers((long) adminUsers.size())
                .activeUsers(allActiveUsers)
                .build();
    }

    public HashMap<String, Long> getActiveUsersCount() {
        ActiveUsersResponse response = getActiveUsers();
        HashMap<String, Long> result = new HashMap<>();
        result.put("totalActiveUsers", response.getTotalActiveUsers());
        result.put("activeRegularUsers", response.getActiveRegularUsers());
        result.put("activeAgencyUsers", response.getActiveAgencyUsers());
        result.put("activeAdminUsers", response.getActiveAdminUsers());
        return result;
    }

    private List<ActiveUserDto> listOnlineUsers() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }

        List<ActiveUserDto> online = new ArrayList<>();
        for (String key : keys) {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                continue;
            }
            try {
                online.add(objectMapper.readValue(json, ActiveUserDto.class));
            } catch (JsonProcessingException e) {
                log.warn("Skipping invalid presence payload for key {}", key);
            }
        }
        return online;
    }

    private Optional<ActiveUserDto> buildSnapshot(String provider, Long userId, String ipAddress, String userAgent) {
        LocalDateTime now = UserDateTimeUtil.now();

        if ("admin".equalsIgnoreCase(provider)) {
            return adminUserRepository.findById(userId)
                    .map(admin -> ActiveUserDto.builder()
                            .userId(admin.getId())
                            .fullName(admin.getFullName())
                            .email(admin.getEmail())
                            .phoneNumber(admin.getPhoneNumber())
                            .userType("ADMIN")
                            .lastLoginAt(now.toString())
                            .ipAddress(ipAddress)
                            .userAgent(userAgent)
                            .isAgency(false)
                            .build());
        }

        return userRepository.findById(userId)
                .map(user -> ActiveUserDto.builder()
                        .userId(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phoneNumber(user.getPhoneNumber())
                        .userType("USER")
                        .lastLoginAt(now.toString())
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .isAgency(user.isAgency())
                        .build());
    }

    private void writePresence(ActiveUserDto dto) {
        String provider = "ADMIN".equalsIgnoreCase(dto.getUserType()) ? "admin" : "user";
        String key = redisKey(provider, dto.getUserId());
        try {
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(presenceTtlSeconds));
            activeUsersBroadcaster.publishActiveUsers();
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize active user presence for user {}", dto.getUserId(), e);
        }
    }

    private String redisKey(String provider, Long userId) {
        return KEY_PREFIX + provider.toLowerCase() + ":" + userId;
    }
}
