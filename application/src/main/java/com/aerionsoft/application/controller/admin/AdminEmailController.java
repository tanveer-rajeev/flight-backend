package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.email.*;
import com.aerionsoft.application.service.admin.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Admin endpoints for sending custom / maintenance emails to users
 * and managing email templates & credentials.
 *
 * Base path: /api/admin/email
 *
 * Example – send maintenance notice to all users:
 *   POST /api/admin/email/broadcast
 *   {
 *     "sendToAll": true,
 *     "subject": "Scheduled Maintenance",
 *     "body": "<h1>We will be down for maintenance on May 13 at 02:00 UTC.</h1>"
 *   }
 *
 * Example – send to specific users by ID:
 *   POST /api/admin/email/broadcast
 *   { "toUserIds": [1, 5, 12], "subject": "...", "body": "..." }
 *
 * Example – send to explicit addresses:
 *   POST /api/admin/email/broadcast
 *   { "toEmails": ["a@b.com"], "subject": "...", "body": "..." }
 *
 * Example – send single / test email:
 *   POST /api/admin/email/send
 *   { "toEmail": "test@example.com", "subject": "Test", "body": "<p>Hello</p>" }
 */
@RestController
@Validated
@RequestMapping("/api/admin/email")
@RequiredArgsConstructor
public class AdminEmailController extends BaseController {

    private final EmailService adminEmailService;

    // ── Broadcast / Bulk ──────────────────────────────────────────────────────

    /**
     * Send a custom or maintenance email to:
     *  - all users (sendToAll = true)
     *  - specific users by userId list (toUserIds)
     *  - explicit email addresses (toEmails)
     * Optionally filter all-users broadcast by role via userType field.
     */
    @PostMapping("/broadcast")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-send-email')")
    public ResponseEntity<BaseResponse<BroadcastEmailResponse>> broadcastEmail(
            @Valid @RequestBody BroadcastEmailRequest request) {
        BroadcastEmailResponse response = adminEmailService.sendBroadcastEmail(request);
        return ResponseEntity.ok(BaseResponse.ok("Email broadcast completed", response));
    }

    // ── Single / test email ───────────────────────────────────────────────────

    /**
     * Send a single email – useful for testing credentials or sending
     * one-off messages to a known address.
     */
    @PostMapping("/send")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-send-email')")
    public ResponseEntity<BaseResponse<EmailResponse>> sendSingle(
            @Valid @RequestBody SingleEmailRequest request) {
        EmailResponse response = adminEmailService.sendSingleEmail(request);
        return ResponseEntity.ok(BaseResponse.ok("Email sent", response));
    }

    // ── Templates ─────────────────────────────────────────────────────────────

    /** List all active email templates (optionally scoped to a businessId). */
    @GetMapping("/templates")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-send-email')")
    public ResponseEntity<BaseResponse<List<EmailTemplateResponse>>> listTemplates(
            @RequestParam(required = false) Long businessId) {
        return ResponseEntity.ok(BaseResponse.ok(adminEmailService.getAllTemplates(businessId)));
    }

    /** Get a single template by ID. */
    @GetMapping("/templates/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-send-email')")
    public ResponseEntity<BaseResponse<EmailTemplateResponse>> getTemplate(
            @PathVariable Long id,
            @RequestParam(required = false) Long businessId) {
        return ResponseEntity.ok(BaseResponse.ok(adminEmailService.getTemplateById(id, businessId)));
    }

    /** Create a new email template. */
    @PostMapping("/templates")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-send-email')")
    public ResponseEntity<BaseResponse<EmailTemplateResponse>> createTemplate(
            @Valid @RequestBody EmailTemplateRequest request) {
        return ResponseEntity.ok(BaseResponse.ok("Template created", adminEmailService.createTemplate(request)));
    }

    /** Update an existing template. */
    @PutMapping("/templates/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-send-email')")
    public ResponseEntity<BaseResponse<EmailTemplateResponse>> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody EmailTemplateRequest request) {
        return ResponseEntity.ok(BaseResponse.ok("Template updated", adminEmailService.updateTemplate(id, request)));
    }

    /** Soft-delete (deactivate) a template. */
    @DeleteMapping("/templates/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-send-email')")
    public ResponseEntity<BaseResponse<String>> deleteTemplate(
            @PathVariable Long id,
            @RequestParam(required = false) Long businessId) {
        adminEmailService.deleteTemplate(id, businessId);
        return ResponseEntity.ok(BaseResponse.ok("Template deleted"));
    }

    // ── Email Credentials ─────────────────────────────────────────────────────

    /** View current active SMTP credentials (password masked). */
    @GetMapping("/credentials")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-send-email')")
    public ResponseEntity<BaseResponse<EmailCredentialsRequest>> getCredentials(
            @RequestParam(required = false) Long businessId) {
        EmailCredentialsRequest creds = adminEmailService.getActiveCredentialsDto(businessId);
        if (creds != null) creds.setPassword(null); // never expose password
        return ResponseEntity.ok(BaseResponse.ok(creds));
    }

    /** Save / update SMTP credentials. */
    @PostMapping("/credentials")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-send-email')")
    public ResponseEntity<BaseResponse<String>> saveCredentials(
            @Valid @RequestBody EmailCredentialsRequest request) {
        adminEmailService.saveEmailCredentials(request);
        return ResponseEntity.ok(BaseResponse.ok("Email credentials saved"));
    }
}

