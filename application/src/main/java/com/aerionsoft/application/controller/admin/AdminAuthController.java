package com.aerionsoft.application.controller.admin;


import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.admin.ChangePasswordRequest;
import com.aerionsoft.application.dto.admin.auth.LoginRequest;
import com.aerionsoft.application.dto.admin.auth.SendLoginOtpRequest;
import com.aerionsoft.application.dto.client.auth.LoginResponse;
import com.aerionsoft.application.dto.client.auth.OtpSend;
import com.aerionsoft.application.dto.client.auth.OtpVerificationRequest;
import com.aerionsoft.application.dto.client.auth.RefreshTokenRequest;
import com.aerionsoft.application.dto.client.auth.RegistrationRequest;
import com.aerionsoft.application.dto.client.user.PasswordResetRequest;
import com.aerionsoft.application.service.admin.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/admin/auth")
public class AdminAuthController extends BaseController {

    @Autowired
    private AdminAuthService authService;

    @PostMapping("/register")
    public ResponseEntity<BaseResponse<?>> register(@Valid @RequestBody RegistrationRequest req) {
        authService.register(req);
        return ResponseEntity.ok(BaseResponse.ok("Registration successful. OTP sent to your email."));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<BaseResponse<?>> verifyOtp(@Valid @RequestBody OtpVerificationRequest req) {
        authService.verifyOtp(req);
        return ResponseEntity.ok(BaseResponse.ok("OTP verified. You can now log in."));
    }

    @PostMapping("/send-login-otp")
    public ResponseEntity<BaseResponse<?>> sendLoginOtp(
            @Valid @RequestBody SendLoginOtpRequest req,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        authService.sendLoginOtp(req, ip, userAgent);
        return ResponseEntity.ok(BaseResponse.ok("OTP sent to your email."));
    }

    @PostMapping("/resend-login-otp")
    public ResponseEntity<BaseResponse<?>> resendLoginOtp(
            @Valid @RequestBody SendLoginOtpRequest req,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        authService.sendLoginOtp(req, ip, userAgent);
        return ResponseEntity.ok(BaseResponse.ok("OTP resent to your email."));
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        LoginResponse loginResponse = authService.login(req, ip, userAgent);

        return ResponseEntity.ok(BaseResponse.ok("Login successful", loginResponse));
    }


    @PostMapping("/reset-password")
    public ResponseEntity<BaseResponse<String>> resetPassword(@Valid @RequestBody PasswordResetRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(BaseResponse.ok("Password reset successful. You can now log in with your new password."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<BaseResponse<?>> forgotPassword(@Valid @RequestBody OtpSend req) {
        authService.forgotPassword(req.getEmail());
        return ResponseEntity.ok(BaseResponse.ok("OTP sent to your email. Please check your inbox."));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<?>> changePassword(Authentication authentication,
                                                          @Valid @RequestBody ChangePasswordRequest req) {
        String email = authentication.getName();
        authService.changePassword(email, req);
        return ResponseEntity.ok(BaseResponse.ok("Password changed successfully."));
    }


    @PostMapping("/refresh-token")
    public ResponseEntity<BaseResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest req,
            HttpServletRequest request) {
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        LoginResponse loginResponse = authService.refreshAccessToken(req.getRefreshToken(), ip, userAgent);
        return ResponseEntity.ok(BaseResponse.ok("Token refreshed successfully", loginResponse));
    }

}
