package com.aerionsoft.application.repository.access;

import com.aerionsoft.application.entity.rolePermission.PermissionGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionGroupRepository extends JpaRepository<PermissionGroup, Long> {
}
