package com.aerionsoft.application.controller.flight;

import com.aerionsoft.application.annotation.AuditedAction;
import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.controller.BaseController;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.MarkupPlanBindingRequest;
import com.aerionsoft.application.dto.MarkupPlanBusinessResponse;
import com.aerionsoft.application.dto.MarkupPlanMultiBulkBindRequest;
import com.aerionsoft.application.entity.MarkupPlan;
import com.aerionsoft.application.entity.MarkupRule;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.service.flight.MarkupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/markup")
public class MarkupController extends BaseController {

    @Autowired
    private MarkupService markupService;

    @PostMapping("/plans")
    @AuditedAction(value = ActivityEventType.MARKUP_PLAN_CREATED, resourceType = "MARKUP_PLAN")
    public ResponseEntity<BaseResponse<MarkupPlan>> createPlan(@Valid @RequestBody MarkupPlan plan) {
        MarkupPlan created = markupService.createMarkupPlan(plan);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Markup plan created successfully", created));
    }

    @GetMapping("/plans")
    public ResponseEntity<BaseResponse<List<MarkupPlan>>> getAllPlans() {
        return ResponseEntity.ok(BaseResponse.ok(markupService.getAllMarkupPlans()));
    }

    @GetMapping("/plans/{id}")
    public ResponseEntity<BaseResponse<MarkupPlan>> getPlan(
            @PathVariable @Positive(message = "Plan id must be a positive number") Long id) {
        MarkupPlan plan = markupService.getMarkupPlan(id);
        if (plan == null) {
            throw new ResourceNotFoundException("Markup plan", id);
        }
        return ResponseEntity.ok(BaseResponse.ok(plan));
    }

    @DeleteMapping("/plans/{id}")
    @AuditedAction(value = ActivityEventType.MARKUP_PLAN_DELETED, resourceType = "MARKUP_PLAN", resourceIdParam = "id")
    public ResponseEntity<BaseResponse<Void>> deletePlan(
            @PathVariable @Positive(message = "Plan id must be a positive number") Long id) {
        markupService.deleteMarkupPlan(id);
        return ResponseEntity.ok(BaseResponse.ok("Markup plan deleted successfully"));
    }

    @PutMapping("/plans/{id}")
    @AuditedAction(value = ActivityEventType.MARKUP_PLAN_UPDATED, resourceType = "MARKUP_PLAN", resourceIdParam = "id")
    public ResponseEntity<BaseResponse<MarkupPlan>> updatePlan(
            @PathVariable @Positive(message = "Plan id must be a positive number") Long id,
            @Valid @RequestBody MarkupPlan plan) {
        MarkupPlan updatedPlan = markupService.updateMarkupPlan(id, plan);
        return ResponseEntity.ok(BaseResponse.ok(updatedPlan));
    }

    @GetMapping("/plans/{id}/rules")
    public ResponseEntity<BaseResponse<List<MarkupRule>>> getRulesByPlanId(
            @PathVariable @Positive(message = "Plan id must be a positive number") Long id) {
        return ResponseEntity.ok(BaseResponse.ok(markupService.getRulesByPlanId(id)));
    }

    @PostMapping("/rules")
    @AuditedAction(value = ActivityEventType.MARKUP_RULE_CREATED, resourceType = "MARKUP_RULE")
    public ResponseEntity<BaseResponse<MarkupRule>> addRule(@Valid @RequestBody MarkupRule rule) {
        MarkupRule created = markupService.addMarkupRule(rule);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Markup rule created successfully", created));
    }

    @PutMapping("/rules/{id}")
    @AuditedAction(value = ActivityEventType.MARKUP_RULE_UPDATED, resourceType = "MARKUP_RULE", resourceIdParam = "id")
    public ResponseEntity<BaseResponse<MarkupRule>> updateRule(
            @PathVariable @Positive(message = "Rule id must be a positive number") Long id,
            @Valid @RequestBody MarkupRule rule) {
        MarkupRule updatedRule = markupService.updateMarkupRule(id, rule);
        return ResponseEntity.ok(BaseResponse.ok(updatedRule));
    }

    @DeleteMapping("/rules/{id}")
    @AuditedAction(value = ActivityEventType.MARKUP_RULE_DELETED, resourceType = "MARKUP_RULE", resourceIdParam = "id")
    public ResponseEntity<BaseResponse<Void>> deleteRule(
            @PathVariable @Positive(message = "Rule id must be a positive number") Long id) {
        markupService.deleteMarkupRule(id);
        return ResponseEntity.ok(BaseResponse.ok("Markup rule deleted successfully"));
    }

    // Business Binding Endpoints

    /**
     * Bind multiple markup plans to a single business.
     * POST /api/markup/businesses/{businessId}/plans
     */
    @PostMapping("/businesses/{businessId}/plans")
    public ResponseEntity<BaseResponse<List<MarkupPlanBusinessResponse>>> bindPlansToBusinesses(
            @PathVariable @Positive(message = "Business id must be a positive number") Long businessId,
            @Valid @RequestBody MarkupPlanBindingRequest request) {
        List<MarkupPlanBusinessResponse> bindings = markupService.bindPlansToBusiness(businessId, request.getMarkupPlanIds());
        return ResponseEntity.ok(BaseResponse.ok(bindings));
    }

    /**
     * Bind multiple markup plans to multiple businesses at once.
     * POST /api/markup/bind-bulk
     * Body: { "markupPlanIds": [1, 2, 3], "businessIds": [101, 102, 103] }
     */
    @PostMapping("/bind-bulk")
    public ResponseEntity<BaseResponse<List<MarkupPlanBusinessResponse>>> bindPlansToBulkBusinesses(
            @Valid @RequestBody MarkupPlanMultiBulkBindRequest request) {
        List<MarkupPlanBusinessResponse> bindings = markupService.bindPlansToBusinesses(
                request.getBusinessIds(), request.getMarkupPlanIds());
        return ResponseEntity.ok(BaseResponse.ok(bindings));
    }

    @GetMapping("/plans/{planId}/businesses")
    public ResponseEntity<BaseResponse<List<MarkupPlanBusinessResponse>>> getBusinessesByPlan(
            @PathVariable @Positive(message = "Plan id must be a positive number") Long planId) {
        return ResponseEntity.ok(BaseResponse.ok(markupService.getBusinessesByPlan(planId)));
    }

    /**
     * List all plan-business bindings with pagination.
     * GET /api/markup/bindings?page=0&size=20&sort=createdAt,desc
     * GET /api/markup/bindings?activeOnly=true&page=0&size=10
     */
    @GetMapping("/bindings")
    public ResponseEntity<BaseResponse<Page<MarkupPlanBusinessResponse>>> getAllBindings(
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(BaseResponse.ok(markupService.getAllBindings(activeOnly, pageable)));
    }

    @DeleteMapping("/plans/{planId}/businesses/{businessId}")
    public ResponseEntity<BaseResponse<Void>> unbindBusinessFromPlan(
            @PathVariable @Positive(message = "Plan id must be a positive number") Long planId,
            @PathVariable @Positive(message = "Business id must be a positive number") Long businessId) {
        markupService.unbindBusinessFromPlan(planId, businessId);
        return ResponseEntity.ok(BaseResponse.ok("Business unbound from markup plan successfully"));
    }

    @GetMapping("/businesses/{businessId}/plans")
    public ResponseEntity<BaseResponse<List<MarkupPlanBusinessResponse>>> getPlansByBusiness(
            @PathVariable @Positive(message = "Business id must be a positive number") Long businessId) {
        return ResponseEntity.ok(BaseResponse.ok(markupService.getPlansByBusiness(businessId)));
    }
}
