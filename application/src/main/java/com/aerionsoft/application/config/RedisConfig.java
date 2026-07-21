package com.aerionsoft.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Double> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Double> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Set key serializer to String
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Set value serializer to Double
        template.setValueSerializer(new GenericToStringSerializer<>(Double.class));
        template.setHashValueSerializer(new GenericToStringSerializer<>(Double.class));

        template.afterPropertiesSet();
        return template;
    }
}

