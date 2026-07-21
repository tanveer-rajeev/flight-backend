package com.aerionsoft.application.repository.user;

import com.aerionsoft.application.entity.LoginHistory;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.entity.client.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    List<LoginHistory> findByUser(User user);
    List<LoginHistory> findByAdminUser(AdminUser user);
    List<LoginHistory> findByUserId(Long userId);
    List<LoginHistory> findByUser_IdIn(List<Long> userIds);

    void deleteByUser(User user);
    void deleteByUserId(Long userId);

    // Find users who logged in after a specific time
    @Query("SELECT DISTINCT lh FROM LoginHistory lh WHERE lh.user IS NOT NULL AND lh.loginAt >= :afterTime ORDER BY lh.loginAt DESC")
    List<LoginHistory> findDistinctUsersByLoginAtAfter(@Param("afterTime") LocalDateTime afterTime);

    // Find admin users who logged in after a specific time
    @Query("SELECT DISTINCT lh FROM LoginHistory lh WHERE lh.adminUser IS NOT NULL AND lh.loginAt >= :afterTime ORDER BY lh.loginAt DESC")
    List<LoginHistory> findDistinctAdminUsersByLoginAtAfter(@Param("afterTime") LocalDateTime afterTime);

    // Count distinct users who logged in after a specific time
    @Query("SELECT COUNT(DISTINCT lh.user.id) FROM LoginHistory lh WHERE lh.user IS NOT NULL AND lh.loginAt >= :afterTime")
    Long countDistinctUsersByLoginAtAfter(@Param("afterTime") LocalDateTime afterTime);

    // Count distinct admin users who logged in after a specific time
    @Query("SELECT COUNT(DISTINCT lh.adminUser.id) FROM LoginHistory lh WHERE lh.adminUser IS NOT NULL AND lh.loginAt >= :afterTime")
    Long countDistinctAdminUsersByLoginAtAfter(@Param("afterTime") LocalDateTime afterTime);

}
