package com.aerionsoft.application.service.access;

import com.aerionsoft.application.dto.rolepermission.response.UserRolePermissionDto;
import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.rolepermission.AssignPermissionDto;
import com.aerionsoft.application.dto.rolepermission.AssignRoleDto;
import com.aerionsoft.application.dto.rolepermission.RoleDto;
import com.aerionsoft.application.dto.rolepermission.response.PermissionResponseDto;
import com.aerionsoft.application.dto.rolepermission.response.RoleResponseDto;
import com.aerionsoft.application.dto.rolepermission.response.RoleWithPermissionDto;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.rolePermission.Permission;
import com.aerionsoft.application.entity.rolePermission.Role;
import com.aerionsoft.application.entity.rolePermission.RoleAssignment;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.access.PermissionRepository;
import com.aerionsoft.application.repository.access.RoleAssignmentRepository;
import com.aerionsoft.application.repository.access.RoleRepository;
import com.aerionsoft.application.service.business.SalesPersonService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository, RoleAssignmentRepository roleAssignmentRepository, UserRepository userRepository, AdminUserRepository adminUserRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.userRepository = userRepository;
        this.adminUserRepository = adminUserRepository;
    }

    /**
     * Get role list service
     *
     * @param provider   Admin Or User
     * @param authUserId Current Authentication user
     * @param page       int
     * @param size       int
     * @return role list
     */
    public List<RoleResponseDto> getFilterRole(String provider, Long authUserId, int page, int size) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);
        Page<Role> roles;

        if (isAdmin) {
            roles = roleRepository.findByAgencyIdIsNull(PageRequest.of(page, size));
        } else {
            User user = userRepository.findById(authUserId).orElseThrow(() -> new ResourceNotFoundException("User"));
            Long parentId = user.getParentUser() != null ? user.getParentUser().getId() : authUserId;

            roles = roleRepository.findByAgencyId(parentId, PageRequest.of(page, size));
        }

        return roles.getContent()
                .stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Get a role with permission service
     *
     * @param provider   admin or user
     * @param authUserId authenticate user id
     * @param roleSlug   find by role slug
     * @return role with permission
     */
    public RoleWithPermissionDto getRoleWithPermission(String provider, Long authUserId, String roleSlug) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);

        if (isAdmin) {
            Role role = roleRepository.findBySlugAndAgencyIdIsNull(roleSlug).orElseThrow(() -> new ResourceNotFoundException("Role"));

            return roleWithPermissionToDto(role);
        } else {
            User user = userRepository.findById(authUserId).orElseThrow(() -> new ResourceNotFoundException("User"));
            Long parentId = user.getParentUser() != null ? user.getParentUser().getId() : authUserId;

            Role role = roleRepository.findBySlugAndAgencyId(roleSlug, parentId).orElseThrow(() -> new ResourceNotFoundException("Role"));

            return roleWithPermissionToDto(role);
        }
    }

    /**
     * Create role service
     *
     * @param provider   admin or agency
     * @param authUserId authenticate user id
     * @param roleDto    request data to create role
     */
    public void createRole(String provider, Long authUserId, RoleDto roleDto) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);

        if (isAdmin) {
            String slug = roleDto.getSlug().isEmpty() ? generateSlug(roleDto.getName()) : roleDto.getSlug();

            Role adminRole = roleRepository.findBySlugAndAgencyIdIsNull(slug).orElse(null);

            if (adminRole != null) {
                throw ServiceExceptions.duplicate("Role already exist: " + roleDto.getName());
            }

            Role role = Role.builder()
                    .name(roleDto.getName())
                    .slug(slug)
                    .isActive(roleDto.getIsActive() != null ? roleDto.getIsActive() : true)
                    .build();
            roleRepository.save(role);
        } else {
            User user = userRepository.findById(authUserId).orElseThrow(() -> new ResourceNotFoundException("User"));
            Long parentId = user.getParentUser() != null ? user.getParentUser().getId() : authUserId;

            String slug = roleDto.getSlug().isEmpty() ? generateSlug(roleDto.getName()) : roleDto.getSlug();

            Role agencyRole = roleRepository.findBySlugAndAgencyId(slug, parentId).orElse(null);

            if (agencyRole != null) {
                throw ServiceExceptions.notFound("Role already exist: " + roleDto.getName());
            }

            Role role = Role.builder()
                    .name(roleDto.getName())
                    .slug(slug)
                    .agencyId(parentId)
                    .isActive(roleDto.getIsActive() != null ? roleDto.getIsActive() : true)
                    .build();
            roleRepository.save(role);
        }
    }

    /**
     * Assign permission to role service
     *
     * @param provider            admin or agency
     * @param authUserId          authenticate user id
     * @param assignPermissionDto request data to set permission to a role
     */
    public void assignPermissionsToRole(String provider, Long authUserId, AssignPermissionDto assignPermissionDto) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);
        Role role;

        if (isAdmin) {
            role = roleRepository.findBySlugAndAgencyIdIsNull(assignPermissionDto.getSlug()).orElseThrow(() -> new ResourceNotFoundException("Role"));
        } else {
            User user = userRepository.findById(authUserId).orElseThrow(() -> new ResourceNotFoundException("User"));
            Long parentId = user.getParentUser() != null ? user.getParentUser().getId() : authUserId;

            role = roleRepository.findBySlugAndAgencyId(assignPermissionDto.getSlug(), parentId).orElseThrow(() -> new ResourceNotFoundException("Role"));
        }

        Set<Permission> permissions = new HashSet<>();

        for (String slug : assignPermissionDto.getPermissions()) {
            Permission permission = permissionRepository.findBySlug(slug)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission", slug));
            permissions.add(permission);
        }

        role.setPermissions(permissions);

        roleRepository.save(role);
    }

    /**
     * Assign role to user service
     *
     * @param provider      admin or user
     * @param authUserId    authenticate user id
     * @param assignRoleDto assign role request data
     */
    public void assignRoleToUser(String provider, Long authUserId, AssignRoleDto assignRoleDto) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);

        if (SalesPersonService.SALES_PERSON_ROLE_SLUG.equalsIgnoreCase(assignRoleDto.getRole())) {
            if (!isAdmin || !adminUserRepository.hasRole(authUserId, "admin")) {
                throw ServiceExceptions.business("Only full admin can assign the sales-person role");
            }
        }

        String userType = assignRoleDto.getEntity();

        if (isAdmin && userType.equalsIgnoreCase("admin")) {
            AdminUser subAdmin = adminUserRepository.findById(assignRoleDto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User"));

            Role role = roleRepository.findBySlugAndAgencyIdIsNull(assignRoleDto.getRole()).orElseThrow(() -> new ResourceNotFoundException("Role"));

            Optional<RoleAssignment> existing = roleAssignmentRepository.findByEntityTypeAndEntityId("ADMIN", subAdmin.getId());

            if (existing.isPresent()) {
                RoleAssignment ra = existing.get();

                if (!ra.getRole().equals(role)) {
                    ra.setRole(role);
                    roleAssignmentRepository.save(ra);
                }
            } else {
                RoleAssignment roleAssignment = new RoleAssignment();
                roleAssignment.setRole(role);
                roleAssignment.setEntityId(subAdmin.getId());
                roleAssignment.setEntityType("ADMIN");
                roleAssignmentRepository.save(roleAssignment);
            }
        } else {
            Role role;
            User user = userRepository.findById(authUserId).orElseThrow(() -> new ResourceNotFoundException("User"));
            Long parentId = user.getParentUser() != null ? user.getParentUser().getId() : authUserId;

            if (isAdmin) {
                role = roleRepository.findBySlug(assignRoleDto.getRole()).orElseThrow(() -> new ResourceNotFoundException("Role"));
            } else {
                role = roleRepository.findBySlugAndAgencyId(assignRoleDto.getRole(), parentId).orElseThrow(() -> new ResourceNotFoundException("Role"));
            }

            Optional<RoleAssignment> existing = roleAssignmentRepository.findByEntityTypeAndEntityId("USER", assignRoleDto.getId());

            if (existing.isPresent()) {
                RoleAssignment ra = existing.get();

                if (!ra.getRole().equals(role)) {
                    ra.setRole(role);
                    roleAssignmentRepository.save(ra);
                }
            } else {
                RoleAssignment roleAssignment = new RoleAssignment();
                roleAssignment.setRole(role);
                roleAssignment.setEntityId(assignRoleDto.getId());
                roleAssignment.setEntityType("USER");
                roleAssignmentRepository.save(roleAssignment);
            }
        }
    }

    /**
     * My role with permissions
     *
     * @param provider admin or user
     * @param authUserId authenticate user id
     * @return RoleWithPermissionDto
     */
    public RoleWithPermissionDto myRoleWithPermissions(String provider, Long authUserId) {
        Optional<RoleAssignment> roleAssignment = roleAssignmentRepository.findByEntityTypeAndEntityId(provider.toUpperCase(), authUserId);

        if (roleAssignment.isEmpty()) {
            Role role = roleRepository.findBySlug("user").orElseThrow(() -> new ResourceNotFoundException("Role"));

            return roleWithPermissionToDto(role);
        }

        RoleAssignment ra = roleAssignment.get();

        Role role = roleRepository.findById(ra.getRole().getId()).orElseThrow(() -> new ResourceNotFoundException("Role"));

        return roleWithPermissionToDto(role);
    }

    public Role getRoleByUserId(Long userId) {
        Optional<RoleAssignment> roleAssignmentOpt = roleAssignmentRepository.findByEntityTypeAndEntityId("USER", userId);
        return roleAssignmentOpt.map(RoleAssignment::getRole).orElse(null);

    }

    public Role getRoleByAdminId(Long userId) {
        Optional<RoleAssignment> roleAssignmentOpt = roleAssignmentRepository.findByEntityTypeAndEntityId("ADMIN", userId);
        return roleAssignmentOpt.map(RoleAssignment::getRole).orElse(null);

    }

    public List<RoleResponseDto> getRolesByAgencyId(Long agencyId, int page, int size) {
        Page<Role> rolesPage = roleRepository.findByAgencyId(agencyId, PageRequest.of(page, size));
        return rolesPage.map(this::toDto).getContent();
    }

    /**
     * Get all permissions for a list of role IDs
     *
     * @param roleIds List of role IDs
     * @return List of unique permissions from all the roles
     */
    public List<PermissionResponseDto> getPermissionsByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Role IDs list cannot be empty");
        }

        List<Role> roles = roleRepository.findAllById(roleIds);

        if (roles.isEmpty()) {
            throw ServiceExceptions.notFound("No roles found for the provided IDs");
        }

        // Collect all permissions from all roles and remove duplicates
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .distinct()
                .map(permission -> {
                    PermissionResponseDto permissionDto = new PermissionResponseDto();
                    permissionDto.setId(permission.getId());
                    permissionDto.setName(permission.getName());
                    permissionDto.setSlug(permission.getSlug());
                    permissionDto.setType(permission.getType());
                    permissionDto.setModule(permission.getModule());
                    permissionDto.setPermissionGroup(permission.getPermissionGroup());
                    permissionDto.setIsActive(permission.getIsActive());
                    return permissionDto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get user's role and permissions by user ID
     *
     * @param userId User ID
     * @return User's role and permissions
     */
    public UserRolePermissionDto getUserRoleAndPermissions(Long userId) {
        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Get role assignment for the user
        Optional<RoleAssignment> roleAssignmentOpt = roleAssignmentRepository.findByEntityTypeAndEntityId("USER", userId);

        if (roleAssignmentOpt.isEmpty()) {
            throw ServiceExceptions.business("No role assigned to user with ID: " + userId);
        }

        RoleAssignment roleAssignment = roleAssignmentOpt.get();
        Role role = roleAssignment.getRole();

        // Build response DTO
        UserRolePermissionDto responseDto =
                new UserRolePermissionDto();
        responseDto.setUserId(user.getId());
        responseDto.setUsername(user.getFullName());
        responseDto.setEmail(user.getEmail());

        // Set role with permissions
        RoleWithPermissionDto roleDto = roleWithPermissionToDto(role);
        responseDto.setRole(roleDto);

        // Set permissions list (extracted from role)
        List<PermissionResponseDto> permissions = role.getPermissions()
                .stream()
                .map(permission -> {
                    PermissionResponseDto permissionDto = new PermissionResponseDto();
                    permissionDto.setId(permission.getId());
                    permissionDto.setName(permission.getName());
                    permissionDto.setSlug(permission.getSlug());
                    permissionDto.setType(permission.getType());
                    permissionDto.setModule(permission.getModule());
                    permissionDto.setPermissionGroup(permission.getPermissionGroup());
                    permissionDto.setIsActive(permission.getIsActive());
                    return permissionDto;
                })
                .collect(Collectors.toList());
        responseDto.setPermissions(permissions);

        return responseDto;
    }

    private RoleResponseDto toDto(Role role) {
        RoleResponseDto dto = new RoleResponseDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setSlug(role.getSlug());
        dto.setIsActive(role.getIsActive());
        return dto;
    }

    private RoleWithPermissionDto roleWithPermissionToDto(Role role) {
        RoleWithPermissionDto dto = new RoleWithPermissionDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setSlug(role.getSlug());
        dto.setIsActive(role.getIsActive());

        List<PermissionResponseDto> permissions = role.getPermissions()
                .stream()
                .map(permission -> {
                    PermissionResponseDto permissionDto = new PermissionResponseDto();
                    permissionDto.setId(permission.getId());
                    permissionDto.setName(permission.getName());
                    permissionDto.setSlug(permission.getSlug());
                    permissionDto.setIsActive(permission.getIsActive());
                    return permissionDto;
                })
                .collect(Collectors.toList());

        dto.setPermissions(permissions);

        return dto;
    }

    /**
     * Generate role slug
     *
     * @param input string
     * @return slug
     */
    private String generateSlug(String input) {
        return input.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }
}