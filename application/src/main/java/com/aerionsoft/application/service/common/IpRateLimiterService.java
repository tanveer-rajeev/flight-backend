package com.aerionsoft.application.service.common;

import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window IP rate limiter. Prefers Redis; falls back to in-memory when Redis is unavailable.
 */
@Service
@Slf4j
public class IpRateLimiterService {

    private static final String KEY_PREFIX = "rate-limit:";

    private final StringRedisTemplate redisTemplate;
    private final Map<String, Deque<Long>> localWindows = new ConcurrentHashMap<>();

    public IpRateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Consumes one attempt for {@code key} from {@code ip}.
     * Throws {@link BusinessException} with {@link ErrorCode#RATE_LIMITED} when the limit is exceeded.
     */
    public void checkOrThrow(String key, String ip, int maxAttempts, Duration window) {
        String clientIp = (ip == null || ip.isBlank()) ? "unknown" : ip.trim();
        String redisKey = KEY_PREFIX + key + ":" + clientIp;

        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(redisKey, window);
            }
            if (count != null && count > maxAttempts) {
                throw new BusinessException(ErrorCode.RATE_LIMITED,
                        "Upload limit exceeded. Maximum " + maxAttempts
                                + " uploads allowed per " + window.toMinutes() + " minutes.");
            }
            return;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis rate limit unavailable, using in-memory fallback: {}", e.getMessage());
        }

        checkLocalOrThrow(redisKey, maxAttempts, window);
    }

    private void checkLocalOrThrow(String key, int maxAttempts, Duration window) {
        long now = System.currentTimeMillis();
        long windowMillis = window.toMillis();

        Deque<Long> timestamps = localWindows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowMillis) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxAttempts) {
                throw new BusinessException(ErrorCode.RATE_LIMITED,
                        "Upload limit exceeded. Maximum " + maxAttempts
                                + " uploads allowed per " + window.toMinutes() + " minutes.");
            }
            timestamps.addLast(now);
        }
    }
}
