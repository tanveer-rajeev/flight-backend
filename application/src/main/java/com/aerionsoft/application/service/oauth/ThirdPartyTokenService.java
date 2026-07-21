package com.aerionsoft.application.service.oauth;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.rolePermission.Role;
import com.aerionsoft.application.entity.rolePermission.RoleAssignment;
import com.aerionsoft.application.enums.user.UserType;
import com.aerionsoft.application.filters.JwtUtil;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.access.RoleAssignmentRepository;
import com.aerionsoft.application.repository.access.RoleRepository;
import com.aerionsoft.application.service.access.RoleService;
import com.aerionsoft.application.util.EmailUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ThirdPartyTokenService {

    private final UserRepository userRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RoleService roleService;
    private final RoleRepository roleRepository;

    public ThirdPartyTokenService(UserRepository userRepository,
                                  RoleAssignmentRepository roleAssignmentRepository,
                                  PasswordEncoder passwordEncoder,
                                  JwtUtil jwtUtil,
                                  RoleService roleService,
                                  RoleRepository roleRepository
    ) {
        this.userRepository = userRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.roleService = roleService;
        this.roleRepository = roleRepository;
    }

    /**
     * Finds user by email; if absent auto-creates a basic active user (unverified).
     * Returns an access token compatible with the existing JwtFilter.
     */
    public TokenResult issueUserToken(String email, String clientId) {
        String normalizedEmail = EmailUtils.normalize(email);
        UserType userType;

        if (clientId.equals("VLife_0001")) {
            userType = UserType.VLIFE;
        } else {
            userType = UserType.NORMAL;
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> autoCreateUser(normalizedEmail, userType));

        // Build authorities from the same role assignment tables used by normal login.
        // JwtFilter expects roles under "authorities" claim (e.g. ROLE_admin, ROLE_user, ROLE_MANAGER).
        Set<Role> assignedRoles = roleAssignmentRepository.findRolesByEntity("USER", user.getId());

        Set<SimpleGrantedAuthority> authorities = assignedRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getSlug()))
                .collect(Collectors.toSet());

        // Backward compatible default if nothing is assigned.
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_user"));
        }

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );

        boolean isAgency = user.isAgency();
        String token = jwtUtil.generateToken(userDetails, "user", isAgency, clientId);

        // NOTE: JwtUtil uses a short TTL; keep expires_in aligned with JwtUtil.ACCESS_TOKEN_TTL_MS.
        return new TokenResult(token, 25L * 60);
    }

    // this auto create will now support vlife role
    // ToDo multiple role support
    private User autoCreateUser(String email, UserType userType) {
        // NOTE: password is randomized (not usable for login) since third party is only minting tokens.
        String randomPassword = passwordEncoder.encode("AUTO_" + java.util.UUID.randomUUID());

        User user = User.builder()
                .email(email)
                .password(randomPassword)
                .fullName(email)
                .isVerified(true)
                .isActive(true)
                .isAgency(false)
                .createdAt(UserDateTimeUtil.now())
                .currency("USD")
                .balance(0.0)
                .userType(userType)
                .roles(Set.of("USER"))
                .build();

        User user1 = userRepository.save(user);

       Role role = roleRepository.findBySlug("vlife").orElseThrow(()-> new ResourceNotFoundException("Role"));

        // Assign role to this user
        assignRole(role, user1.getId());

        return user1;
    }

    public record TokenResult(String accessToken, long expiresInSeconds) {
    }

    private void assignRole(Role role, Long userId) {
        RoleAssignment roleAssignment = new RoleAssignment();
        roleAssignment.setRole(role);
        roleAssignment.setEntityId(userId);
        roleAssignment.setEntityType("USER");

        roleAssignmentRepository.save(roleAssignment);
    }
}
