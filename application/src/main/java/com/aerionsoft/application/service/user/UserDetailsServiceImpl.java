package com.aerionsoft.application.service.user;

import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.access.RoleAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AdminUserRepository adminUserRepo;

    @Autowired
    private RoleAssignmentRepository roleAssignmentRepo;

    private UserDetails buildUserDetails(Long id, String email, String password, boolean isVerified, boolean isActive, List<String> roles, String provider) {
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        if (!isActive) {
            throw new UsernameNotFoundException("User is not active");
        }

        return new CustomUserDetails(
                id,
                email,
                password,
                isVerified,
                isActive,
                authorities,
                provider
        );
    }

    public UserDetails loadUserByEmail(String email, String provider) throws UsernameNotFoundException {
        Long entityId;
        String entityType;

        if (provider.equalsIgnoreCase("admin")) {
            AdminUser adminUser = adminUserRepo.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Admin User not found"));
            entityId = adminUser.getId();
            entityType = "USER";

            List<String> roles = roleAssignmentRepo.findRolesByEntity(entityType, entityId)
                    .stream()
                    .map(role -> role.getSlug())
                    .toList();

            return buildUserDetails(
                    adminUser.getId(),
                    adminUser.getEmail(),
                    adminUser.getPassword(),
                    adminUser.getIsVerified(),
                    adminUser.getIsActive(),
                    roles,
                    "admin"
            );
        }

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        entityId = user.getId();
        entityType = "USER";

        List<String> roles = roleAssignmentRepo.findRolesByEntity(entityType, entityId)
                .stream()
                .map(role -> role.getSlug())
                .toList();

        return buildUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.isVerified(),
                user.getIsActive(),
                roles,
                "user"
        );
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return loadUserByEmail(username, "user");
    }
}