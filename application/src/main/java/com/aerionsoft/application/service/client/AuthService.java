package com.aerionsoft.application.service.client;

import com.aerionsoft.application.entity.RefreshToken;
import com.aerionsoft.application.util.ActorContext;
import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.client.auth.LoginRequest;
import com.aerionsoft.application.dto.client.auth.LoginResponse;
import com.aerionsoft.application.dto.client.auth.OtpVerificationRequest;
import com.aerionsoft.application.dto.client.auth.RegistrationRequest;
import com.aerionsoft.application.dto.client.user.PasswordResetRequest;
import com.aerionsoft.application.entity.LoginHistory;
import com.aerionsoft.application.entity.OtpToken;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.rolePermission.Role;
import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.filters.JwtUtil;
import com.aerionsoft.application.repository.user.LoginHistoryRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.common.OtpTokenRepository;
import com.aerionsoft.application.repository.access.RoleAssignmentRepository;
import com.aerionsoft.application.service.audit.ActivityAuthAuditSupport;
import com.aerionsoft.application.service.user.ActiveUserPresenceService;
import com.aerionsoft.application.service.common.EmailService;
import com.aerionsoft.application.service.notification.NotificationHelper;
import com.aerionsoft.application.util.EmailUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Service
@Slf4j
public class AuthService extends BaseController {
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private OtpTokenRepository otpTokenRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private LoginHistoryRepository loginHistoryRepo;

    @Autowired
    private RoleAssignmentRepository roleAssignmentRepo;

    @Autowired
    @Qualifier("generalEmailService")
    private EmailService emailService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private NotificationHelper notificationHelper;

    @Autowired
    private ActiveUserPresenceService presenceService;

    @Autowired
    private ActivityAuthAuditSupport activityAuthAuditSupport;

    public void register(RegistrationRequest req) {

        String email = EmailUtils.normalize(req.getEmail());


        if (userRepo.findByEmail(email).isPresent())
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "User already exists!");
        Set<String> defaultRoles = new HashSet<>();
        defaultRoles.add("USER");

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(req.getPassword()))
                .isVerified(false)
                .fullName(req.getFullName())
                .isActive(true)
                .createdAt(UserDateTimeUtil.now())
                .isAgency(false)
                .currency(Currency.fromCode(req.getCurrencyCode()))
                .balance(0.0)
                .phoneNumber(req.getPhone())
                .roles(defaultRoles)
                .ticketPreview("compact")
                .build();
        userRepo.save(user);

        sendOtp(user);
    }

    public void sendOtp(User user) {
        // Throttle: Only allow sending OTP if last OTP was sent more than 2 minutes ago (for user only)
        if (user != null) {
            OtpToken lastOtp = otpTokenRepo.findTopByUserOrderByCreatedAtDesc(user);
            if (lastOtp != null && lastOtp.getCreatedAt().isAfter(UserDateTimeUtil.now().minusMinutes(2))) {
                throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "OTP already sent recently. Please wait 2 minutes before requesting again.");
            }
        }
        String otp = String.format("%06d", new Random().nextInt(999999));
        OtpToken otpToken = OtpToken.builder().user(user).otpCode(otp).expiresAt(UserDateTimeUtil.now().plusMinutes(5))
                .createdAt(UserDateTimeUtil.now()).used(false).build();
        otpTokenRepo.save(otpToken);

        // Send OTP via email using database credentials
        emailService.sendOtp(user.getEmail(), otp);
    }

    public void sendOtp(String email) {
        String normalizedEmail = EmailUtils.normalize(email);
        // 1. Find user by email
        Optional<User> optionalUser = userRepo.findByEmail(normalizedEmail);
        if (optionalUser.isEmpty()) {
            throw ServiceExceptions.notFound("User not found with email: " + normalizedEmail);
        }

        User user = optionalUser.get();

        // 2. Throttle: prevent resending within 2 minutes
        OtpToken lastOtp = otpTokenRepo.findTopByUserOrderByCreatedAtDesc(user);
        if (lastOtp != null && lastOtp.getCreatedAt().isAfter(UserDateTimeUtil.now().minusMinutes(2))) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "OTP already sent recently. Please wait 2 minutes before requesting again.");
        }

        // 3. Generate OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // 4. Save OTP token
        OtpToken otpToken = OtpToken.builder()
                .user(user)
                .otpCode(otp)
                .expiresAt(UserDateTimeUtil.now().plusMinutes(5))
                .createdAt(UserDateTimeUtil.now())
                .used(false)
                .build();
        otpTokenRepo.save(otpToken);

        // 5. Send OTP email
        emailService.sendOtp(normalizedEmail, otp);
    }


    public void verifyOtp(OtpVerificationRequest req) {

        String email = EmailUtils.normalize(req.getEmail());



        User user = userRepo.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User"));

        OtpToken otpToken = otpTokenRepo.findByUserAndOtpCodeAndUsedIsFalse(user, req.getOtp())
                .orElseThrow(() -> ServiceExceptions.unauthorized("Invalid OTP"));

        if (otpToken.getExpiresAt().isBefore(UserDateTimeUtil.now()))
            throw ServiceExceptions.unauthorized("OTP expired!");

        otpToken.setUsed(true);
        otpTokenRepo.save(otpToken);

        user.setVerified(true);
        userRepo.save(user);

        // Send account verification success notification with email
        try {
            notificationHelper.sendWelcomeNotification(user.getId(), user.getEmail(), user.getFullName(), true);

        } catch (Exception e) {
            // Log error but don't block verification process
            log.error("Failed to send verification notification for user {}: {}", user.getId(), e.getMessage());
        }
    }

    public LoginResponse login(LoginRequest req, String ip, String userAgent) {

        String emailLower = EmailUtils.normalize(req.getEmail());

        Optional<User> userOpt = userRepo.findByEmail(emailLower);
        if (userOpt.isEmpty()) {
            activityAuthAuditSupport.logLoginFailed(emailLower, "User not found", false, ip, userAgent);
            throw new ResourceNotFoundException("User");
        }

        User user = userOpt.get();

        if (user.isDeleted()) {
            activityAuthAuditSupport.logLoginFailed(emailLower, "Account is deleted", false, ip, userAgent);
            throw ServiceExceptions.unauthorized("Account is deleted!");
        }

        if (!user.isVerified()) {
            activityAuthAuditSupport.logLoginFailed(emailLower, "Account not verified", false, ip, userAgent);
            throw ServiceExceptions.unauthorized("Account not verified!");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(emailLower, req.getPassword()));
        } catch (BadCredentialsException ex) {
            activityAuthAuditSupport.logLoginFailed(emailLower, "Invalid credentials", false, ip, userAgent);
            throw ex;
        }

        Set<Role> userRoles = roleAssignmentRepo.findRolesByEntity( "USER", user.getId());


        Set<SimpleGrantedAuthority> authorities = userRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getSlug()))
                .collect(java.util.stream.Collectors.toSet());

        if (userRoles.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_user"));
        }


        UserDetails userDetails = new org.springframework.security.core.userdetails.User(user.getEmail(),
                user.getPassword(), authorities);


        String token = jwtUtil.generateToken(userDetails, "user",user.isAgency());

        // Generate refresh token with IP and user agent tracking
        String refreshToken = refreshTokenService.createRefreshToken(user, ip, userAgent).getToken();

        loginHistoryRepo.save(LoginHistory.builder()
                .user(user)
                .loginAt(UserDateTimeUtil.now())
                .ipAddress(ip)
                .userAgent(userAgent)
                .build());

        activityAuthAuditSupport.logUserLogin(user, ip, userAgent);

        presenceService.markOnline("user", user.getId(), ip, userAgent);

        return new LoginResponse(token, refreshToken);
    }

    public void resetPassword(PasswordResetRequest req) {

        String email = EmailUtils.normalize(req.getEmail());
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        OtpToken otpToken = otpTokenRepo.findByUserAndOtpCodeAndUsedIsFalse(user, req.getOtp())
                .orElseThrow(() -> ServiceExceptions.unauthorized("Invalid OTP"));
        if (otpToken.getExpiresAt().isBefore(UserDateTimeUtil.now())) {
            throw ServiceExceptions.unauthorized("OTP expired!");
        }
        otpToken.setUsed(true);
        otpTokenRepo.save(otpToken);
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepo.save(user);
        activityAuthAuditSupport.logPasswordReset(
                ActorContext.forUser(user.getId(), user.getEmail()),
                "USER",
                user.getId());
    }

    public LoginResponse refreshAccessToken(String refreshTokenString, String ip, String userAgent) {
        // Validate refresh token
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(refreshTokenString);

        // Get user from refresh token
        User user = refreshToken.getUser();

        // Check if user is still active and verified
        if (user.isDeleted()) {
            throw ServiceExceptions.unauthorized("Account is deleted!");
        }

        if (!user.isVerified()) {
            throw ServiceExceptions.unauthorized("Account not verified!");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw ServiceExceptions.unauthorized("Account is not active!");
        }

        // Get user roles
        Set<Role> userRoles = roleAssignmentRepo.findRolesByEntity("USER", user.getId());

        Set<SimpleGrantedAuthority> authorities = userRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getSlug()))
                .collect(java.util.stream.Collectors.toSet());

        if (userRoles.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_user"));
        }

        // Create UserDetails
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );

        // Generate new access token
        String newAccessToken = jwtUtil.generateToken(userDetails, "user", user.isAgency());

        // activityAuthAuditSupport.logTokenRefresh(
        //         com.aerionsoft.application.util.ActorContext.forUser(user.getId(), user.getEmail()),
        //         ip,
        //         userAgent);

        // Return response with new access token and same refresh token
        return new LoginResponse(newAccessToken);
    }

}
