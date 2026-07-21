package com.aerionsoft.application.service.admin;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.admin.ImpersonateUserRequest;
import com.aerionsoft.application.dto.admin.ImpersonateUserResponse;
import com.aerionsoft.application.entity.LoginHistory;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.rolePermission.Role;
import com.aerionsoft.application.filters.JwtUtil;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.user.LoginHistoryRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.access.RoleAssignmentRepository;
import com.aerionsoft.application.service.audit.ActivityAuthAuditSupport;
import com.aerionsoft.application.service.user.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminImpersonationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private RoleAssignmentRepository roleAssignmentRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Autowired
    private ActivityAuthAuditSupport activityAuthAuditSupport;

    public ImpersonateUserResponse impersonate(CustomUserDetails adminPrincipal,
                                              ImpersonateUserRequest request,
                                              String ip,
                                              String userAgent) {
        if (adminPrincipal == null || adminPrincipal.getProvider() == null || !adminPrincipal.getProvider().equalsIgnoreCase("admin")) {
            throw ServiceExceptions.unauthorized("Only admin users can impersonate.");
        }

        AdminUser adminUser = adminUserRepository.findById(adminPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Admin user"));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        if (user.isDeleted()) {
            throw ServiceExceptions.unauthorized("User account is deleted");
        }
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw ServiceExceptions.unauthorized("User account is not active");
        }

        Set<Role> userRoles = roleAssignmentRepository.findRolesByEntity("USER", user.getId());
        Set<SimpleGrantedAuthority> authorities = userRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getSlug()))
                .collect(Collectors.toSet());

        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_user"));
        }

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );

        String token = jwtUtil.generateImpersonationToken(userDetails, adminUser.getId(), user.isAgency());

        // Audit: store as a login history record tied to both admin + user
        loginHistoryRepository.save(LoginHistory.builder()
                .user(user)
                .adminUser(adminUser)
                .loginAt(UserDateTimeUtil.now())
                .ipAddress(ip)
                .userAgent(userAgent)
                .build());

        activityAuthAuditSupport.logImpersonation(adminUser, user, ip, userAgent, request.getReason());

        return new ImpersonateUserResponse(token);
    }
}
