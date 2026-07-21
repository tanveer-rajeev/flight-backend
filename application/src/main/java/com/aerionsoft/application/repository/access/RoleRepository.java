package com.aerionsoft.application.repository.access;

import com.aerionsoft.application.entity.rolePermission.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findBySlugAndAgencyIdIsNull(String slug);
    Optional<Role> findBySlugAndAgencyId(String slug,  Long agencyId);

    Page<Role> findByAgencyId(Long agencyId, Pageable pageable);

    Page<Role> findByAgencyIdIsNull(Pageable pageable);

    Optional<Role> findBySlug(String roleSlug);
}
