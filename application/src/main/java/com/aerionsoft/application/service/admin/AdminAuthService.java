package com.aerionsoft.application.service.admin;

import com.aerionsoft.application.dto.admin.ChangePasswordRequest;
import com.aerionsoft.application.dto.client.user.PasswordResetRequest;
import com.aerionsoft.application.entity.RefreshToken;
import com.aerionsoft.application.util.ActorContext;
import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.admin.auth.LoginRequest;
import com.aerionsoft.application.dto.admin.auth.SendLoginOtpRequest;
import com.aerionsoft.application.dto.client.auth.LoginResponse;
import com.aerionsoft.application.dto.client.auth.OtpVerificationRequest;
import com.aerionsoft.application.dto.client.auth.RegistrationRequest;
import com.aerionsoft.application.entity.LoginHistory;
import com.aerionsoft.application.entity.OtpToken;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.entity.rolePermission.Role;
import com.aerionsoft.application.filters.JwtUtil;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.user.LoginHistoryRepository;
import com.aerionsoft.application.repository.common.OtpTokenRepository;
import com.aerionsoft.application.repository.access.RoleAssignmentRepository;
import com.aerionsoft.application.service.audit.ActivityAuthAuditSupport;
import com.aerionsoft.application.service.user.ActiveUserPresenceService;
import com.aerionsoft.application.service.common.EmailService;
import com.aerionsoft.application.util.EmailUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@Service
public class AdminAuthService {

    @Autowired
    AdminUserRepository adminUserRepo;
    @Autowired
    private OtpTokenRepository otpTokenRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    @Qualifier("generalEmailService")
    private EmailService emailService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private LoginHistoryRepository loginHistoryRepo;

    @Autowired
    private RoleAssignmentRepository roleAssignmentRepo;

    @Autowired
    private AdminRefreshTokenService adminRefreshTokenService;

    @Autowired
    private ActiveUserPresenceService presenceService;

    @Autowired
    private ActivityAuthAuditSupport activityAuthAuditSupport;

    public void register(RegistrationRequest req) {
        String email = EmailUtils.normalize(req.getEmail());
        if (adminUserRepo.findByEmail(email).isPresent())
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "User already exists!");

        Set<String> defaultRoles = new HashSet<>();
        defaultRoles.add("ADMIN");

        AdminUser user = AdminUser.builder()
                .email(email)
                .password(passwordEncoder.encode(req.getPassword()))
                .isVerified(false)
                .fullName(req.getFullName())
                .createdAt(UserDateTimeUtil.now())
                .isActive(true)
                .roles(defaultRoles)
                .build();
        adminUserRepo.save(user);

        sendOtp(user);
    }

    public void sendOtp(AdminUser adminUser) {
        OtpToken lastOtp = otpTokenRepo.findTopByAdminUserOrderByCreatedAtDesc(adminUser);
        if (lastOtp != null && lastOtp.getCreatedAt().isAfter(UserDateTimeUtil.now().minusMinutes(2))) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE,
                    "OTP already sent recently. Please wait 2 minutes before requesting again.");
        }

        String otp = String.format("%06d", new Random().nextInt(999998));
        OtpToken otpToken = OtpToken.builder()
                .adminUser(adminUser)
                .otpCode(otp).expiresAt(UserDateTimeUtil.now().plusMinutes(5)).createdAt(UserDateTimeUtil.now()).used(false)
                .build();
        otpTokenRepo.save(otpToken);

        // Send OTP via email using database credentials
        emailService.sendOtp(adminUser.getEmail(), otp);
    }

    public void sendLoginOtp(SendLoginOtpRequest req, String ip, String userAgent) {
        AdminUser adminUser = validateAdminCredentials(req.getEmail(), req.getPassword(), ip, userAgent);
        sendOtp(adminUser);
    }

    public void verifyOtp(OtpVerificationRequest req) {
        String email = EmailUtils.normalize(req.getEmail());
        AdminUser adminUser = adminUserRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        OtpToken otpToken = otpTokenRepo.findByAdminUserAndOtpCodeAndUsedIsFalse(adminUser, req.getOtp())
                .orElseThrow(() -> ServiceExceptions.unauthorized("Invalid OTP"));

        if (otpToken.getExpiresAt().isBefore(UserDateTimeUtil.now()))
            throw ServiceExceptions.unauthorized("OTP expired!");

        otpToken.setUsed(true);
        otpTokenRepo.save(otpToken);

        adminUser.setIsVerified(true);
        adminUserRepo.save(adminUser);
    }

    public LoginResponse login(LoginRequest req, String ip, String userAgent) {
        AdminUser adminUser = validateAdminCredentials(req.getEmail(), req.getPassword(), ip, userAgent);
        validateLoginOtp(adminUser, req.getOtp());

        Set<Role> userRoles = roleAssignmentRepo.findRolesByEntity("ADMIN", adminUser.getId());

        Set<SimpleGrantedAuthority> authoritiesForAdmin = new HashSet<>();
        for (Role role : userRoles) {
            authoritiesForAdmin.add(new SimpleGrantedAuthority("ROLE_" + role.getSlug()));
        }


        UserDetails userDetails = new org.springframework.security.core.userdetails.User(adminUser.getEmail(),
                adminUser.getPassword(), authoritiesForAdmin);

        String token = jwtUtil.generateToken(userDetails, "admin", false);

        // Generate refresh token with IP and user agent tracking
        String refreshToken = adminRefreshTokenService.createRefreshToken(adminUser, ip, userAgent).getToken();

        loginHistoryRepo.save(LoginHistory.builder()
                .adminUser(adminUser)
                .loginAt(UserDateTimeUtil.now())
                .ipAddress(ip)
                .userAgent(userAgent)
                .build());

        activityAuthAuditSupport.logAdminLogin(adminUser, ip, userAgent);

        presenceService.markOnline("admin", adminUser.getId(), ip, userAgent);

        return new LoginResponse(token, refreshToken);
    }

    public void forgotPassword(String email) {
        String normalizedEmail = EmailUtils.normalize(email);
        AdminUser adminUser = adminUserRepo.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        sendOtp(adminUser);
    }

    public void resetPassword(PasswordResetRequest req) {
        String email = EmailUtils.normalize(req.getEmail());
        AdminUser adminUser = adminUserRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        OtpToken otpToken = otpTokenRepo.findByAdminUserAndOtpCodeAndUsedIsFalse(adminUser, req.getOtp())
                .orElseThrow(() -> ServiceExceptions.unauthorized("Invalid OTP"));

        if (otpToken.getExpiresAt().isBefore(UserDateTimeUtil.now()))
            throw ServiceExceptions.unauthorized("OTP expired!");

        otpToken.setUsed(true);
        otpTokenRepo.save(otpToken);

        adminUser.setPassword(passwordEncoder.encode(req.getNewPassword()));
        adminUserRepo.save(adminUser);
        activityAuthAuditSupport.logPasswordReset(
                ActorContext.forAdmin(adminUser.getId(), adminUser.getEmail()),
                "ADMIN_USER",
                adminUser.getId());
    }

    public void changePassword(String email, ChangePasswordRequest req) {
        AdminUser adminUser = adminUserRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        if (!passwordEncoder.matches(req.getOldPassword(), adminUser.getPassword())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Invalid old password");
        }

        adminUser.setPassword(passwordEncoder.encode(req.getNewPassword()));
        adminUserRepo.save(adminUser);
        activityAuthAuditSupport.logPasswordChange(
                ActorContext.forAdmin(adminUser.getId(), adminUser.getEmail()),
                "ADMIN_USER",
                adminUser.getId());
    }

    public LoginResponse refreshAccessToken(String refreshTokenString, String ip, String userAgent) {
        // Validate refresh token
        RefreshToken refreshToken = adminRefreshTokenService.validateRefreshToken(refreshTokenString);

        // Get admin user from refresh token
        AdminUser adminUser = refreshToken.getAdminUser();

        // Check if admin user is still active and verified
        if (!Boolean.TRUE.equals(adminUser.getIsVerified())) {
            throw ServiceExceptions.unauthorized("Account not verified!");
        }

        if (!Boolean.TRUE.equals(adminUser.getIsActive())) {
            throw ServiceExceptions.unauthorized("Account is not active!");
        }

        // Get admin user roles
        Set<Role> userRoles = roleAssignmentRepo.findRolesByEntity("ADMIN", adminUser.getId());

        Set<SimpleGrantedAuthority> authoritiesForAdmin = new HashSet<>();
        for (Role role : userRoles) {
            authoritiesForAdmin.add(new SimpleGrantedAuthority("ROLE_" + role.getSlug()));
        }

        // Generate new access token
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                adminUser.getEmail(),
                adminUser.getPassword(),
                authoritiesForAdmin
        );

        String newAccessToken = jwtUtil.generateToken(userDetails, "admin", false);

        // activityAuthAuditSupport.logTokenRefresh(
        //         com.aerionsoft.application.util.ActorContext.forAdmin(adminUser.getId(), adminUser.getEmail()),
        //         ip,
        //         userAgent);

        // Return new access token with same refresh token
        return new LoginResponse(newAccessToken);
    }

    private AdminUser validateAdminCredentials(String email, String password, String ip, String userAgent) {
        String normalizedEmail = EmailUtils.normalize(email);
        AdminUser adminUser = adminUserRepo.findByEmail(normalizedEmail).orElse(null);

        if (adminUser == null) {
            activityAuthAuditSupport.logLoginFailed(normalizedEmail, "User not found", true, ip, userAgent);
            throw new ResourceNotFoundException("User");
        }

        if (!adminUser.getIsVerified()) {
            activityAuthAuditSupport.logLoginFailed(normalizedEmail, "Account not verified", true, ip, userAgent);
            throw ServiceExceptions.unauthorized("Account not verified!");
        }

        if (!Boolean.TRUE.equals(adminUser.getIsActive())) {
            activityAuthAuditSupport.logLoginFailed(normalizedEmail, "Account is not active", true, ip, userAgent);
            throw ServiceExceptions.unauthorized("Account is not active!");
        }

        if (!passwordEncoder.matches(password, adminUser.getPassword())) {
            activityAuthAuditSupport.logLoginFailed(normalizedEmail, "Invalid password", true, ip, userAgent);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Invalid password");
        }

        return adminUser;
    }

    private void validateLoginOtp(AdminUser adminUser, String otp) {
        OtpToken otpToken = otpTokenRepo.findByAdminUserAndOtpCodeAndUsedIsFalse(adminUser, otp)
                .orElseThrow(() -> ServiceExceptions.unauthorized("Invalid OTP"));

        if (otpToken.getExpiresAt().isBefore(UserDateTimeUtil.now())) {
            throw ServiceExceptions.unauthorized("OTP expired!");
        }

        otpToken.setUsed(true);
        otpTokenRepo.save(otpToken);
    }

}
