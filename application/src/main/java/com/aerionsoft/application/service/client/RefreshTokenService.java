package com.aerionsoft.application.service.client;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.entity.RefreshToken;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.filters.JwtUtil;
import com.aerionsoft.application.repository.user.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Transactional
    public RefreshToken createRefreshToken(User user, String ipAddress, String userAgent) {
        // Generate refresh token
        String tokenString = jwtUtil.generateRefreshToken(user.getEmail());

        // Calculate expiration time
        long ttlMs = jwtUtil.getRefreshTokenTtlMs();
        LocalDateTime expiresAt = UserDateTimeUtil.now().plusSeconds(ttlMs / 1000);

        // Create refresh token entity
        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenString)
                .user(user)
                .expiresAt(expiresAt)
                .createdAt(UserDateTimeUtil.now())
                .revoked(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken validateRefreshToken(String token) {
        // First validate JWT structure and expiration
        if (!jwtUtil.isRefreshToken(token)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Invalid refresh token type");
        }

        // Find token in database
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> ServiceExceptions.unauthorized("Invalid or revoked refresh token"));

        // Check if expired
        if (refreshToken.getExpiresAt().isBefore(UserDateTimeUtil.now())) {
            throw ServiceExceptions.unauthorized("Refresh token has expired");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token"));
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        List<RefreshToken> userTokens = refreshTokenRepository.findByUserAndRevokedFalse(user);
        userTokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(userTokens);
    }

    // Scheduled task to clean up expired tokens (runs daily at 2 AM)
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(UserDateTimeUtil.now());
    }
}
