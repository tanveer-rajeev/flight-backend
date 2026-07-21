package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.webhook.WebhookAlertConfigRequest;
import com.aerionsoft.application.dto.webhook.WebhookAlertConfigResponse;
import com.aerionsoft.application.dto.webhook.WebhookAlertMetaResponse;
import com.aerionsoft.application.service.webhook.WebhookAlertConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/admin/webhook-alert-configs")
@RequiredArgsConstructor
public class WebhookAlertConfigAdminController extends BaseController {

    private final WebhookAlertConfigService webhookAlertConfigService;

    @GetMapping("/meta")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-webhook-alert-config')")
    public ResponseEntity<BaseResponse<WebhookAlertMetaResponse>> getMeta() {
        return ResponseEntity.ok(BaseResponse.ok(webhookAlertConfigService.getMeta(),
                "Webhook alert metadata retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-webhook-alert-config')")
    public ResponseEntity<BaseResponse<WebhookAlertConfigResponse>> create(
            @Valid @RequestBody WebhookAlertConfigRequest request) {
        WebhookAlertConfigResponse response = webhookAlertConfigService.create(request, currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Webhook alert config created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-webhook-alert-config')")
    public ResponseEntity<BaseResponse<WebhookAlertConfigResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody WebhookAlertConfigRequest request) {
        WebhookAlertConfigResponse response = webhookAlertConfigService.update(id, request, currentUserId());
        return ResponseEntity.ok(BaseResponse.ok(response, "Webhook alert config updated successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-webhook-alert-config')")
    public ResponseEntity<BaseResponse<WebhookAlertConfigResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.ok(webhookAlertConfigService.getById(id),
                "Webhook alert config retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-webhook-alert-config')")
    public ResponseEntity<BaseResponse<List<WebhookAlertConfigResponse>>> getAll(
            @RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(BaseResponse.ok(webhookAlertConfigService.getAll(activeOnly),
                "Webhook alert configs retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-webhook-alert-config')")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable Long id) {
        webhookAlertConfigService.delete(id);
        return ResponseEntity.ok(BaseResponse.ok("Webhook alert config deleted successfully"));
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-webhook-alert-config')")
    public ResponseEntity<BaseResponse<Map<String, String>>> sendTest(@PathVariable Long id) {
        webhookAlertConfigService.sendTestAlert(id);
        return ResponseEntity.ok(BaseResponse.ok(
                Map.of("message", "Test webhook alert dispatched"),
                "Test webhook alert sent successfully"));
    }
}
