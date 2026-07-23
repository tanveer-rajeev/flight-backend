package com.aerionsoft.application.repository.common;

import com.aerionsoft.application.entity.OtpToken;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.entity.client.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findByUserAndOtpCodeAndUsedIsFalse(User user, String otpCode);
    Optional<OtpToken> findByAdminUserAndOtpCodeAndUsedIsFalse(AdminUser adminUser, String otpCode);

    OtpToken findTopByUserOrderByCreatedAtDesc(User user);

    OtpToken findTopByAdminUserOrderByCreatedAtDesc(AdminUser adminUser);

    Optional<OtpToken> findTopByAdminUserAndUsedIsFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            AdminUser adminUser, LocalDateTime expiresAt);

    void deleteByUser(User user);

    void deleteByAdminUser(AdminUser adminUser);
}