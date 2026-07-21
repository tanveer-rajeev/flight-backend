package com.aerionsoft.application.config;

import com.aerionsoft.application.websocket.StompAuthChannelInterceptor;
import com.aerionsoft.application.websocket.WebSocketAuthHandshakeInterceptor;
import com.aerionsoft.application.websocket.WebSocketTopics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthHandshakeInterceptor handshakeInterceptor;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    public WebSocketConfig(WebSocketAuthHandshakeInterceptor handshakeInterceptor,
                           StompAuthChannelInterceptor stompAuthChannelInterceptor) {
        this.handshakeInterceptor = handshakeInterceptor;
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
    }

    /**
     * Required for STOMP heartbeats on the simple broker. Without a scheduler,
     * @stomp/stompjs clients connect then drop into a reconnect loop.
     */
    @Bean
    public TaskScheduler webSocketHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(webSocketHeartbeatScheduler());
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Canonical: /ws  | Alias: /api/ws (frontends that concatenate apiBase + "/ws")
        registry.addEndpoint(WebSocketTopics.ENDPOINT, WebSocketTopics.ENDPOINT_API_ALIAS)
                .setAllowedOriginPatterns("*")
                .addInterceptors(handshakeInterceptor)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
        // High-TPS chat: larger pool so /app/chat/send is not serialized on a tiny default executor
        registration.taskExecutor()
                .corePoolSize(16)
                .maxPoolSize(64)
                .queueCapacity(2000);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                .corePoolSize(16)
                .maxPoolSize(64)
                .queueCapacity(2000);
    }
}
