package com.aerionsoft.application.config;

import java.util.Optional;

import com.aerionsoft.application.util.UserDateTimeUtil;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

@Configuration
public class UserDateTimeProviderConfig {

    @Bean
    public DateTimeProvider userDateTimeProvider() {
        return () -> Optional.of(UserDateTimeUtil.now());
    }
}
