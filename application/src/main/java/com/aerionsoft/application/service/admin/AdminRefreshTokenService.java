package com.aerionsoft.application.service.admin;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.entity.RefreshToken;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.filters.JwtUtil;
import com.aerionsoft.application.repository.user.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminRefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Transactional
    public RefreshToken createRefreshToken(AdminUser adminUser, String ipAddress, String userAgent) {
        // Generate refresh token
        String tokenString = jwtUtil.generateRefreshToken(adminUser.getEmail());

        // Calculate expiration time
        long ttlMs = jwtUtil.getRefreshTokenTtlMs();
        LocalDateTime expiresAt = UserDateTimeUtil.now().plusSeconds(ttlMs / 1000);

        // Create refresh token entity
        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenString)
                .adminUser(adminUser)
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

        // Ensure it's an admin token
        if (refreshToken.getAdminUser() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Invalid admin refresh token");
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
    public void revokeAllAdminUserTokens(AdminUser adminUser) {
        List<RefreshToken> adminUserTokens = refreshTokenRepository.findByAdminUserAndRevokedFalse(adminUser);
        adminUserTokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(adminUserTokens);
    }
}

