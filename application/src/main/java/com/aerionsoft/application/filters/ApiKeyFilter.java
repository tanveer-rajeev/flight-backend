package com.aerionsoft.application.filters;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.service.admin.ApiKeyService;
import com.aerionsoft.application.util.PublicRoutes;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Order(2)
public class ApiKeyFilter extends OncePerRequestFilter {
    private static final String API_KEY_HEADER = "X-API-KEY";


    private static final List<String> ANT_STYLE_PATTERNS_FOR_SUPER_KEY = List.of(
            "/api/core/callback/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ApiKeyService apiKeyService;

    public ApiKeyFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        String path = request.getRequestURI();
        String apiKey = request.getHeader(API_KEY_HEADER);

        // Skip API key validation for unprotected paths
        if (!isProtectedPath(path) && !isSuperProtectedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Super-protected paths require a super key
        if (isSuperProtectedPath(path)) {
            if (apiKey == null || !apiKeyService.isValidSuperKey(apiKey)) {
                writeErrorResponse(response, BaseResponse.error(
                        HttpStatus.UNAUTHORIZED.value(), "Invalid super API key", null));
                return;
            }
        } else {
            // Regular protected paths require a normal key
            if (apiKey == null || !apiKeyService.isValidKey(apiKey)) {
                writeErrorResponse(response, BaseResponse.error(
                        HttpStatus.UNAUTHORIZED.value(), "Invalid API key", null));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isProtectedPath(String path) {
        return PublicRoutes.EXACT_MATCH_PATHS.contains(path)
                || PublicRoutes.ANT_STYLE_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isSuperProtectedPath(String path) {
        return ANT_STYLE_PATTERNS_FOR_SUPER_KEY.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !isProtectedPath(path) && !isSuperProtectedPath(path);
    }

    private void writeErrorResponse(HttpServletResponse response, BaseResponse<?> baseResponse) throws IOException {
        response.setStatus(baseResponse.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(baseResponse));
    }
}
