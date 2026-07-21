package com.aerionsoft.application.controller.client;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.client.auth.*;
import com.aerionsoft.application.dto.client.user.PasswordResetRequest;
import com.aerionsoft.application.service.client.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/auth")
public class AuthController extends BaseController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<BaseResponse<?>> register(@Valid @RequestBody RegistrationRequest req) {
        authService.register(req);
        return ResponseEntity.ok(BaseResponse.ok("Registration successful."));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<BaseResponse<?>> verifyOtp(@Valid @RequestBody OtpVerificationRequest req) {
        authService.verifyOtp(req);
        return ResponseEntity.ok(BaseResponse.ok("OTP verified. You can now log in."));
    }
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        LoginResponse loginResponse = authService.login(req, ip, userAgent);

        return ResponseEntity.ok(BaseResponse.ok("Login successful", loginResponse));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<BaseResponse<?>> resetPassword(@Valid @RequestBody PasswordResetRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(BaseResponse.ok("Password reset successful."));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<BaseResponse<?>> sendOtp(@Valid @RequestBody OtpSend otpSend) {
        authService.sendOtp(otpSend.getEmail());
        return ResponseEntity.ok(BaseResponse.ok("OTP Sent Successfully."));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<BaseResponse<?>> resendOtp(@Valid @RequestBody OtpSend otpSend) {
        authService.sendOtp(otpSend.getEmail());
        return ResponseEntity.ok(BaseResponse.ok("OTP resent successfully."));
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
