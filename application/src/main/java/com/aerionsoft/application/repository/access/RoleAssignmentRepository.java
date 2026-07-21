package com.aerionsoft.application.repository.access;

import com.aerionsoft.application.entity.rolePermission.Role;
import com.aerionsoft.application.entity.rolePermission.RoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, Long> {
    @Query("SELECT ra.role FROM RoleAssignment ra WHERE ra.entityType = :entityType AND ra.entityId = :entityId")
    Set<Role> findRolesByEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    boolean existsByEntityTypeAndEntityIdAndRole(String entityType, Long entityId, Role role);

    Optional<RoleAssignment> findByEntityTypeAndEntityId(String entityType, Long entityId);

    Optional<RoleAssignment> findByEntityIdAndEntityTypeAndRole(Long entityId, String entityType, Role role);

    void deleteByEntityIdAndEntityTypeAndRole(Long entityId, String entityType, Role role);

    @Query("""
        SELECT ra.entityId FROM RoleAssignment ra
        JOIN ra.role r
        WHERE UPPER(ra.entityType) = 'ADMIN'
          AND (LOWER(r.slug) = LOWER(:slug) OR LOWER(r.name) = LOWER(:roleName))
        """)
    List<Long> findAdminEntityIdsByRoleSlugOrName(@Param("slug") String slug,
                                                  @Param("roleName") String roleName);
}
