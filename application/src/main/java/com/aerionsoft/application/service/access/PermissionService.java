package com.aerionsoft.application.service.access;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.rolepermission.PermissionDto;
import com.aerionsoft.application.dto.rolepermission.response.PermissionResponseDto;
import com.aerionsoft.application.entity.rolePermission.Permission;
import com.aerionsoft.application.entity.rolePermission.PermissionGroup;
import com.aerionsoft.application.entity.rolePermission.Role;
import com.aerionsoft.application.entity.rolePermission.RoleAssignment;
import com.aerionsoft.application.enums.access.PermissionModule;
import com.aerionsoft.application.enums.access.PermissionType;
import com.aerionsoft.application.repository.access.PermissionGroupRepository;
import com.aerionsoft.application.repository.access.PermissionRepository;
import com.aerionsoft.application.repository.access.RoleAssignmentRepository;
import com.aerionsoft.application.repository.access.RoleRepository;
import com.aerionsoft.application.service.user.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service("permissionService")
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final PermissionGroupRepository permissionGroupRepository;

    public PermissionService(PermissionRepository permissionRepository, RoleRepository roleRepository, RoleAssignmentRepository roleAssignmentRepository, PermissionGroupRepository permissionGroupRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.permissionGroupRepository = permissionGroupRepository;
    }

    // List all permissions
    public Map<String, List<PermissionResponseDto>> getAllPermissions(String module, String type, Long permissionGroupId) {
        PermissionModule moduleEnum = null;

        if (module != null && !module.isBlank()) {
            moduleEnum = PermissionModule.valueOf(module.toUpperCase());
        }

        PermissionType typeEnum = null;
        if (type != null && !type.isBlank()) {
            typeEnum = PermissionType.valueOf(type.toUpperCase());
        }

        return permissionRepository.searchPermissions(moduleEnum, typeEnum, permissionGroupId)
                .stream()
                .map(permission -> {
                    PermissionResponseDto dto = new PermissionResponseDto();
                    dto.setId(permission.getId());
                    dto.setName(permission.getName());
                    dto.setSlug(permission.getSlug());
                    dto.setIsActive(permission.getIsActive());
                    dto.setType(permission.getType());
                    dto.setModule(permission.getModule());
                    dto.setPermissionGroup(permission.getPermissionGroup());
                    return dto;
                })
                .collect(Collectors.groupingBy(
                        dto -> dto.getPermissionGroup() != null
                                ? dto.getPermissionGroup().getName()
                                : "Ungrouped"
                ));

    }

    // Create new permission
    public void createPermission(PermissionDto permissionDto) {

        PermissionGroup pGroup = permissionGroupRepository.findById(permissionDto.getPermissionGroupId()).orElseThrow(()-> new ResourceNotFoundException("Permission group"));

        Permission permission = Permission.builder()
                .name(permissionDto.getName())
                .slug(permissionDto.getSlug())
                .type(PermissionType.valueOf(permissionDto.getType()))
                .module(PermissionModule.valueOf(permissionDto.getModule()))
                .isActive(permissionDto.getIsActive() != null ? permissionDto.getIsActive() : true)
                .permissionGroup(pGroup)
                .build();

        permissionRepository.save(permission);
    }

    // Update permission
    public void updatePermission(Long id, PermissionDto permissionDto) {
        Permission permission = permissionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Permission"));
        PermissionGroup pGroup = permissionGroupRepository.findById(permissionDto.getPermissionGroupId()).orElseThrow(()-> new ResourceNotFoundException("Permission group"));

        if (permission.getModule() == null) {
            permission.setName(permissionDto.getName());
            permission.setType(PermissionType.valueOf(permissionDto.getType()));
            permission.setModule(PermissionModule.valueOf(permissionDto.getModule()));
            permission.setPermissionGroup(pGroup);

            permissionRepository.save(permission);
        } else {
            permission.setName(permissionDto.getName());
            permission.setSlug(permissionDto.getSlug());
            permission.setType(PermissionType.valueOf(permissionDto.getType()));
            permission.setModule(PermissionModule.valueOf(permissionDto.getModule()));
            permission.setIsActive(permissionDto.getIsActive() != null ? permissionDto.getIsActive() : true);
            permission.setPermissionGroup(pGroup);

            permissionRepository.save(permission);
        }
    }

    // Get permission by slug
    public Permission getPermissionBySlug(String slug) {
        return permissionRepository.findBySlug(slug)
                .orElseThrow(() -> ServiceExceptions.notFound("Permission not found with slug: " + slug));
    }

    /** True only for users with the global {@code admin} role slug (not sub-admins). */
    public boolean isFullAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_admin".equalsIgnoreCase(a.getAuthority()));
    }

    // Check if authenticated user has permission
    public boolean hasPermission(Authentication authentication, String permissionSlug) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        Long userId = principal.getId();
        String provider = principal.getProvider();

        // Extract roles from JWT Authentication
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null || authorities.isEmpty()) {
            return false;
        }

        // Check Permission of each role
        for (GrantedAuthority authority : authorities) {
            String roleSlug = authority.getAuthority().replace("ROLE_", "");

            if (roleSlug.equals("user")) {
                Optional<Role> roleOpt = roleRepository.findBySlug(roleSlug);

                if (roleOpt.isEmpty()) continue;

                Role role = roleOpt.get();

                boolean hasPerm = role.getPermissions().stream()
                        .anyMatch(p -> p.getSlug().equalsIgnoreCase(permissionSlug));

                if (hasPerm) {
                    return true;
                }

            } else {
                Optional<RoleAssignment> roleAssignment = roleAssignmentRepository.findByEntityTypeAndEntityId(provider.toUpperCase(), userId);

                if (roleAssignment.isEmpty()) {
                    continue;
                }

                RoleAssignment ra = roleAssignment.get();
                Role role = ra.getRole();

                if (role == null) continue;

                boolean hasPerm = role.getPermissions().stream()
                        .anyMatch(p -> p.getSlug().equalsIgnoreCase(permissionSlug));

                if (hasPerm) {
                    return true;
                }
            }
        }

        return false;
    }

    public List<PermissionResponseDto> getMenuPermissions(String provider, Long authUserId) {
        RoleAssignment roleAssignment = roleAssignmentRepository
                .findByEntityTypeAndEntityId(provider.toUpperCase(), authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Role"));
        Role  role = roleAssignment.getRole();

       return role.getPermissions().stream()
                .filter(permission -> permission.getModule() == PermissionModule.MENU)
                .map(this::mapPermissionResponseDto)
                .toList();
    }

    private PermissionResponseDto mapPermissionResponseDto(Permission permission) {
        PermissionResponseDto permissionResponseDto = new PermissionResponseDto();

        permissionResponseDto.setId(permission.getId());
        permissionResponseDto.setName(permission.getName());
        permissionResponseDto.setSlug(permission.getSlug());
        permissionResponseDto.setType(permission.getType());
        permissionResponseDto.setModule(permission.getModule());
        permissionResponseDto.setPermissionGroup(permission.getPermissionGroup());
        permissionResponseDto.setIsActive(permission.getIsActive());

        return permissionResponseDto;
    }
}
