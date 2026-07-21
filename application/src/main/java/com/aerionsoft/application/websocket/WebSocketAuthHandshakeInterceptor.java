package com.aerionsoft.application.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

@Component
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    public static final String AUTH_ATTRIBUTE = "WS_AUTH";

    private final WebSocketAuthService webSocketAuthService;

    public WebSocketAuthHandshakeInterceptor(WebSocketAuthService webSocketAuthService) {
        this.webSocketAuthService = webSocketAuthService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            attributes.put(WebSocketTopics.IP_ATTRIBUTE, servletRequest.getServletRequest().getRemoteAddr());
            attributes.put(WebSocketTopics.USER_AGENT_ATTRIBUTE,
                    servletRequest.getServletRequest().getHeader("User-Agent"));
        }

        Optional<Authentication> authentication = resolveAuthentication(request);
        authentication.ifPresent(auth -> attributes.put(AUTH_ATTRIBUTE, auth));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }

    private Optional<Authentication> resolveAuthentication(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String authHeader = servletRequest.getServletRequest().getHeader("Authorization");
            Optional<Authentication> fromHeader = webSocketAuthService.authenticateToken(authHeader);
            if (fromHeader.isPresent()) {
                return fromHeader;
            }

            String tokenParam = servletRequest.getServletRequest().getParameter("token");
            return webSocketAuthService.authenticateToken(tokenParam);
        }
        return Optional.empty();
    }
}
