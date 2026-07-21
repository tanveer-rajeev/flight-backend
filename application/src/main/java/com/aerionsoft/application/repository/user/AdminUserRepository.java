package com.aerionsoft.application.repository.user;

import com.aerionsoft.application.entity.admin.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByEmail(String email);

    @Query("""
        SELECT DISTINCT au FROM AdminUser au
        JOIN RoleAssignment ra ON ra.entityId = au.id AND UPPER(ra.entityType) = 'ADMIN'
        JOIN ra.role r
        WHERE au.isActive = true
          AND (LOWER(r.slug) = LOWER(:slug) OR LOWER(r.name) = LOWER(:roleName))
        """)
    List<AdminUser> findSalesPersons(@Param("slug") String slug, @Param("roleName") String roleName);

    @Query("""
        SELECT COUNT(au) > 0 FROM AdminUser au
        JOIN RoleAssignment ra ON ra.entityId = au.id AND UPPER(ra.entityType) = 'ADMIN'
        JOIN ra.role r
        WHERE au.id = :adminId
          AND (LOWER(r.slug) = LOWER(:slug) OR LOWER(r.name) = LOWER(:roleName))
        """)
    boolean hasSalesPersonRole(@Param("adminId") Long adminId,
                               @Param("slug") String slug,
                               @Param("roleName") String roleName);

    default boolean hasSalesPersonRole(Long adminId) {
        return hasSalesPersonRole(adminId, "sales-person", "Sales Person");
    }

    @Query("SELECT DISTINCT au FROM AdminUser au " +
            "JOIN RoleAssignment ra ON ra.entityId = au.id " +
            "JOIN Role r ON r = ra.role " +
            "WHERE ra.entityType = 'ADMIN' AND r.slug = :slug")
    List<AdminUser> findAdminsByRoleSlug(@Param("slug") String slug);

    @Query("""
    SELECT COUNT(au) > 0 
    FROM AdminUser au
    JOIN RoleAssignment ra ON ra.entityId = au.id
    JOIN Role r ON r = ra.role
    WHERE ra.entityType = 'ADMIN'
      AND au.id = :adminId
      AND r.slug = :slug
    """)
    boolean hasRole(@Param("adminId") Long adminId, @Param("slug") String slug);

    @Query("""
    SELECT DISTINCT au
    FROM AdminUser au
    WHERE NOT EXISTS (
        SELECT 1
        FROM RoleAssignment ra
        JOIN ra.role r
        WHERE ra.entityId = au.id
          AND ra.entityType = 'ADMIN'
          AND UPPER(r.slug) = 'ADMIN'
      ) AND (:currency IS NULL OR UPPER(au.currency) = :currency)
    """)
    List<AdminUser> findActiveAdminsWithoutAdminRole(@Param("currency") String currency);

    @Modifying
    @Query("DELETE FROM AdminUser au WHERE au.isVerified = false AND au.createdAt < :cutoffTime")
    int deleteByIsVerifiedFalseAndCreatedAtBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
}