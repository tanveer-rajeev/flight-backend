package com.aerionsoft.application.repository.access;

import com.aerionsoft.application.entity.rolePermission.Permission;
import com.aerionsoft.application.enums.access.PermissionModule;
import com.aerionsoft.application.enums.access.PermissionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findBySlug(String slug);

    @Query("""
        SELECT p FROM Permission p
        WHERE (:module IS NULL OR p.module = :module)
          AND (:type IS NULL OR p.type = :type)
          AND (:permissionGroupId IS NULL OR p.permissionGroup.id = :permissionGroupId)
    """)
    List<Permission> searchPermissions(
            @Param("module") PermissionModule module,
            @Param("type") PermissionType type,
            @Param("permissionGroupId") Long permissionGroupId
    );

    List<Permission> findByModule(PermissionModule module);
}
