package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.annotation.SkipAutoAudit;
import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.admin.ImpersonateUserRequest;
import com.aerionsoft.application.dto.admin.ImpersonateUserResponse;
import com.aerionsoft.application.service.admin.AdminImpersonationService;
import com.aerionsoft.application.service.user.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SkipAutoAudit
@RestController
@Validated
@RequestMapping("/api/admin/impersonation")
public class ImpersonationController extends BaseController {

    @Autowired
    private AdminImpersonationService adminImpersonationService;

    @PostMapping
    @PreAuthorize("hasRole('admin') or @permissionService.hasPermission(authentication, 'impersonate-user')")
    public ResponseEntity<BaseResponse<ImpersonateUserResponse>> impersonate(
            @Valid @RequestBody ImpersonateUserRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();

        ImpersonateUserResponse response = adminImpersonationService.impersonate(principal, request, ip, userAgent);
        return ResponseEntity.ok(BaseResponse.ok("Impersonation successful", response));
    }
}
