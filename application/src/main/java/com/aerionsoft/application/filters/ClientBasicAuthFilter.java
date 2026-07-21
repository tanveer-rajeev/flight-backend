package com.aerionsoft.application.filters;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.service.oauth.OAuthClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Protects /api/oauth/token with HTTP Basic client credentials.
 *
 * This is NOT user authentication; it's third-party client authentication.
 */
@Component
public class ClientBasicAuthFilter extends OncePerRequestFilter {

    private static final String TOKEN_PATH = "/api/oauth/token";

    private final OAuthClientService oAuthClientService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClientBasicAuthFilter(OAuthClientService oAuthClientService) {
        this.oAuthClientService = oAuthClientService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !TOKEN_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Basic ")) {
            writeUnauthorized(response, "Missing Basic Authorization header");
            return;
        }

        String base64 = auth.substring(6).trim();
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            writeUnauthorized(response, "Invalid Basic Authorization header");
            return;
        }

        int idx = decoded.indexOf(':');
        if (idx <= 0) {
            writeUnauthorized(response, "Invalid Basic Authorization header");
            return;
        }

        String clientId = decoded.substring(0, idx);
        String clientSecret = decoded.substring(idx + 1);

        if (!oAuthClientService.isValidClient(clientId, clientSecret)) {
            writeUnauthorized(response, "invalid_client");
            return;
        }

        // Put client id in request for downstream logging/use if needed
        request.setAttribute("oauthClientId", clientId);

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("WWW-Authenticate", "Basic realm=\"oauth\"");
        response.getWriter().write(objectMapper.writeValueAsString(BaseResponse.error(
                HttpStatus.UNAUTHORIZED.value(), message, null
        )));
    }
}

