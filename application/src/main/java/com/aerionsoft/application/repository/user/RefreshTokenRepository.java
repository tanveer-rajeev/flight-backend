package com.aerionsoft.application.repository.user;

import com.aerionsoft.application.entity.RefreshToken;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.entity.client.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    List<RefreshToken> findByUserAndRevokedFalse(User user);

    List<RefreshToken> findByAdminUserAndRevokedFalse(AdminUser adminUser);

    void deleteByExpiresAtBefore(LocalDateTime now);

    void deleteByUser(User user);

    void deleteByAdminUser(AdminUser adminUser);
}
