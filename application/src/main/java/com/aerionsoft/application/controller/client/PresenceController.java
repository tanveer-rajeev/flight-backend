package com.aerionsoft.application.controller.client;

import org.springframework.validation.annotation.Validated;
import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.service.user.ActiveUserPresenceService;
import com.aerionsoft.application.service.user.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/presence")
public class PresenceController extends BaseController {

    private final ActiveUserPresenceService presenceService;

    public PresenceController(ActiveUserPresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<BaseResponse<Void>> heartbeat(Authentication authentication,
                                                        HttpServletRequest request) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            String ip = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            presenceService.recordActivity(details, ip, userAgent);
        }
        return ResponseEntity.ok(BaseResponse.ok("Presence updated"));
    }
}
