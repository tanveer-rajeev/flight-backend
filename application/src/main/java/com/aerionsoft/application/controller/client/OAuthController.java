package com.aerionsoft.application.controller.client;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.client.auth.LoginResponse;
import com.aerionsoft.application.dto.oauth.LoginWithVlifeResponse;
import com.aerionsoft.application.service.oauth.VLifeOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/vlife/oauth")
public class OAuthController {

    private final VLifeOAuthService vLifeOAuthService;

    @Value("${vlife.frontend_url:#{null}}")
    private String frontendUrl;

    public OAuthController(VLifeOAuthService vLifeOAuthService) {
        this.vLifeOAuthService = vLifeOAuthService;
    }

    @GetMapping("/callback")
    public void getAccessToken(@RequestParam String code, @RequestParam String state, HttpServletResponse httpServletResponse) throws Exception {
        LoginResponse loginResponse = vLifeOAuthService.getAccessToken(code, state);
        String token = loginResponse.getToken();

        String frontendUrl = this.frontendUrl + "?token=" + token;

        httpServletResponse.sendRedirect(frontendUrl);
    }

    @PostMapping("/login-with-vlife")
    public ResponseEntity<BaseResponse<LoginWithVlifeResponse>> loginWithVLife() {
        LoginWithVlifeResponse response = vLifeOAuthService.loginWithVlife();

        return ResponseEntity.ok(BaseResponse.ok("", response));
    }
}
