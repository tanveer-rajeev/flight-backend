package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.email.*;
import com.aerionsoft.application.dto.notification.NotificationTemplateRequest;
import com.aerionsoft.application.dto.notification.NotificationTemplateResponse;
import com.aerionsoft.application.dto.sms.BulkSmsRequest;
import com.aerionsoft.application.dto.sms.SingleSmsRequest;
import com.aerionsoft.application.dto.sms.SmsCredentialsRequest;
import com.aerionsoft.application.dto.sms.SmsResponse;
import com.aerionsoft.application.service.admin.EmailService;
import com.aerionsoft.application.service.admin.SmsService;
import com.aerionsoft.application.service.notification.NotificationTemplateService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/admin/communication")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('admin')")
public class CommunicationController {

    @Qualifier("adminEmailService")
    private final EmailService emailService;
    private final SmsService smsService;
    private final NotificationTemplateService notificationTemplateService;

    // Email Endpoints
    @PostMapping("/email/send-single")
    public ResponseEntity<BaseResponse<EmailResponse>> sendSingleEmail(@Valid @RequestBody SingleEmailRequest request) {
        EmailResponse response = emailService.sendSingleEmail(request);
        return ResponseEntity.ok(BaseResponse.ok(response, "Email sent successfully"));
    }

    @PostMapping("/email/send-bulk")
    public ResponseEntity<BaseResponse<List<EmailResponse>>> sendBulkEmail(@Valid @RequestBody BulkEmailRequest request) {
        List<EmailResponse> responses = emailService.sendBulkEmail(request);
        return ResponseEntity.ok(BaseResponse.ok(responses, "Bulk emails sent successfully"));
    }

    // Email Template Endpoints
    @PostMapping("/email/templates")
    public ResponseEntity<BaseResponse<EmailTemplateResponse>> createEmailTemplate(@Valid @RequestBody EmailTemplateRequest request) {
        EmailTemplateResponse response = emailService.createTemplate(request);
        return ResponseEntity.ok(BaseResponse.ok(response, "Email template created successfully"));
    }

    @PutMapping("/email/templates/{id}")
    public ResponseEntity<BaseResponse<EmailTemplateResponse>> updateEmailTemplate(
            @PathVariable Long id,
            @Valid @RequestBody EmailTemplateRequest request) {
        EmailTemplateResponse response = emailService.updateTemplate(id, request);
        return ResponseEntity.ok(BaseResponse.ok(response, "Email template updated successfully"));
    }

    @DeleteMapping("/email/templates/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteEmailTemplate(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "0") Long businessId) {
        emailService.deleteTemplate(id, businessId);
        return ResponseEntity.ok(BaseResponse.ok("Email template deleted successfully"));
    }

    @GetMapping("/email/templates")
    public ResponseEntity<BaseResponse<List<EmailTemplateResponse>>> getAllEmailTemplates(
            @RequestParam(required = false, defaultValue = "0") Long businessId) {
        List<EmailTemplateResponse> responses = emailService.getAllTemplates(businessId);
        return ResponseEntity.ok(BaseResponse.ok(responses, "Email templates retrieved successfully"));
    }

    // Notification Template Endpoints
    @PostMapping("/notification/templates")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-notification-template')")
    public ResponseEntity<BaseResponse<NotificationTemplateResponse>> createNotificationTemplate(
            @Valid @RequestBody NotificationTemplateRequest request) {
        NotificationTemplateResponse response = notificationTemplateService.createTemplate(request);
        return ResponseEntity.ok(BaseResponse.ok(response, "Notification template created successfully"));
    }

    @PutMapping("/notification/templates/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-notification-template')")
    public ResponseEntity<BaseResponse<NotificationTemplateResponse>> updateNotificationTemplate(
            @PathVariable Long id,
            @Valid @RequestBody NotificationTemplateRequest request) {
        NotificationTemplateResponse response = notificationTemplateService.updateTemplate(id, request);
        return ResponseEntity.ok(BaseResponse.ok(response, "Notification template updated successfully"));
    }

    @DeleteMapping("/notification/templates/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-notification-template')")
    public ResponseEntity<BaseResponse<Void>> deleteNotificationTemplate(@PathVariable Long id) {
        notificationTemplateService.deleteTemplate(id);
        return ResponseEntity.ok(BaseResponse.ok("Notification template deleted successfully"));
    }

    @GetMapping("/notification/templates")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-notification-template')")
    public ResponseEntity<BaseResponse<List<NotificationTemplateResponse>>> getAllNotificationTemplates(
            @RequestParam(required = false) Boolean active) {
        List<NotificationTemplateResponse> responses = notificationTemplateService.getAllTemplates(active);
        return ResponseEntity.ok(BaseResponse.ok(responses, "Notification templates retrieved successfully"));
    }

    @GetMapping("/notification/templates/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-notification-template')")
    public ResponseEntity<BaseResponse<NotificationTemplateResponse>> getNotificationTemplate(@PathVariable Long id) {
        NotificationTemplateResponse response = notificationTemplateService.getTemplateById(id);
        return ResponseEntity.ok(BaseResponse.ok(response, "Notification template retrieved successfully"));
    }

    @GetMapping("/notification/templates/code/{templateCode}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-notification-template')")
    public ResponseEntity<BaseResponse<NotificationTemplateResponse>> getNotificationTemplateByCode(
            @PathVariable String templateCode) {
        NotificationTemplateResponse response = notificationTemplateService.getTemplateByCode(templateCode);
        return ResponseEntity.ok(BaseResponse.ok(response, "Notification template retrieved successfully"));
    }

    // Email Credentials Endpoint
    @PostMapping("/email/credentials")
    public ResponseEntity<BaseResponse<Void>> saveEmailCredentials(@Valid @RequestBody EmailCredentialsRequest request) {
        emailService.saveEmailCredentials(request);
        return ResponseEntity.ok(BaseResponse.ok("Email credentials saved successfully"));
    }

    // SMS Endpoints
    @PostMapping("/sms/send-single")
    public ResponseEntity<BaseResponse<SmsResponse>> sendSingleSms(@Valid @RequestBody SingleSmsRequest request) {
        SmsResponse response = smsService.sendSingleSms(request);
        return ResponseEntity.ok(BaseResponse.ok(response, "SMS sent successfully"));
    }

    @PostMapping("/sms/send-bulk")
    public ResponseEntity<BaseResponse<SmsResponse>> sendBulkSms(@Valid @RequestBody BulkSmsRequest request) {
        SmsResponse response = smsService.sendBulkSms(request);
        return ResponseEntity.ok(BaseResponse.ok(response, "Bulk SMS sent successfully"));
    }

    // SMS Credentials Endpoint
    @PostMapping("/sms/credentials")
    public ResponseEntity<BaseResponse<Void>> saveSmsCredentials(@Valid @RequestBody SmsCredentialsRequest request) {
        smsService.saveSmsCredentials(request);
        return ResponseEntity.ok(BaseResponse.ok("SMS credentials saved successfully"));
    }
}
