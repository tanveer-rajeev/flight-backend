package com.aerionsoft.application.controller.oauth;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.service.oauth.ThirdPartyTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/oauth")
public class ThirdPartyTokenController {

    private final ThirdPartyTokenService thirdPartyTokenService;

    public ThirdPartyTokenController(ThirdPartyTokenService thirdPartyTokenService) {
        this.thirdPartyTokenService = thirdPartyTokenService;
    }

    /**
     * Token endpoint for third-party integration.
     *
     * Requires HTTP Basic Auth: client_id/client_secret
     * Accepts: application/x-www-form-urlencoded
     *   grantType=token&email=user@example.com
     */
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public BaseResponse<?> token(
            @RequestParam("grantType") @NotBlank String grantType,
            @RequestParam("email") @NotBlank @Email String email,
            HttpServletRequest request
    ) {
        if (!"token".equalsIgnoreCase(grantType)) {
            return BaseResponse.error(400, "unsupported_grant_type", Map.of("grantType", grantType));
        }
        String clientId = (String) request.getAttribute("oauthClientId");

        ThirdPartyTokenService.TokenResult result = thirdPartyTokenService.issueUserToken(email,clientId);

        return BaseResponse.ok(Map.of(
                "token_type", "Bearer",
                "access_token", result.accessToken(),
                "expires_in", result.expiresInSeconds()
        ));
    }
}

