package com.aerionsoft.application.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Markup rules cache - refreshed every 5 minutes
        cacheManager.registerCustomCache("markupRules",
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .recordStats()
                        .build());

        // User data cache - refreshed every 10 minutes
        cacheManager.registerCustomCache("userData",
                Caffeine.newBuilder()
                        .maximumSize(5000)
                        .expireAfterWrite(Duration.ofMinutes(10))
                        .recordStats()
                        .build());

        // Markup context cache - short-lived for request processing
        cacheManager.registerCustomCache("markupContext",
                Caffeine.newBuilder()
                        .maximumSize(10000)
                        .expireAfterWrite(Duration.ofMinutes(1))
                        .recordStats()
                        .build());

        return cacheManager;
    }
}
