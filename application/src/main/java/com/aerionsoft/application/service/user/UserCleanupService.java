package com.aerionsoft.application.service.user;

import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.repository.user.RefreshTokenRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.common.OtpTokenRepository;
import com.aerionsoft.application.service.notification.NotificationHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(UserCleanupService.class);

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final NotificationHelper notificationHelper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupUser(User user) {
        // Clean up related OTP tokens
        otpTokenRepository.deleteByUser(user);

        // Clean up related refresh tokens (if any)
        refreshTokenRepository.deleteByUser(user);

        // Delete the user account
        userRepository.delete(user);

        // Notify admin about user deletion (optional)
        // notificationHelper.sendSystemAlert(1L, "Unverified user deleted: " + user.getEmail(), NotificationPriority.LOW);

        logger.debug("Deleted unverified user: {} (created at: {})",
                user.getEmail(), user.getCreatedAt());
    }
}
