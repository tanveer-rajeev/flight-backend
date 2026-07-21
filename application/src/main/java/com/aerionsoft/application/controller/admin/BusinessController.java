package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.annotation.AuditedAction;
import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.business.BusinessDto;
import com.aerionsoft.application.dto.business.BusinessRequest;
import com.aerionsoft.application.dto.UpdateBusinessRequest;
import com.aerionsoft.application.dto.UpdateBusinessStatusRequest;
import com.aerionsoft.application.dto.client.user.UserDto;
import com.aerionsoft.application.dto.salesperson.SalesPersonResponseDto;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.service.business.BusinessProviderService;
import com.aerionsoft.application.service.business.BusinessSalesPersonService;
import com.aerionsoft.application.service.business.BusinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/admin/businesses")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;
    private final BusinessProviderService businessProviderService;
    private final BusinessSalesPersonService businessSalesPersonService;

    // ── existing endpoints ──────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-business')")
    public ResponseEntity<BaseResponse<Page<BusinessDto>>> getAllBusinesses(
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(BaseResponse.ok("Businesses fetched successfully",
                businessService.getAllBusinesses(currency, query, page, size)));
    }

    @PostMapping("/create")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-business')")
    public ResponseEntity<BaseResponse<BusinessDto>> createBusiness(@Valid @RequestBody BusinessRequest request) {
        return ResponseEntity.ok(BaseResponse.ok("Business created successfully",
                businessService.createBusiness(request)));
    }

    @GetMapping("/{businessId}/users")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-business-user')")
    public ResponseEntity<BaseResponse<List<UserDto>>> getUsersOfBusiness(@PathVariable Long businessId) {
        return ResponseEntity.ok(BaseResponse.ok("Business users fetched successfully",
                businessService.getUsersOfBusiness(businessId)));
    }

    @PutMapping("/{businessId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-business')")
    public ResponseEntity<BaseResponse<BusinessDto>> updateBusiness(@PathVariable Long businessId, @Valid @RequestBody UpdateBusinessRequest request) {
        return ResponseEntity.ok(BaseResponse.ok("Business updated successfully",
                businessService.updateBusiness(businessId, request)));
    }

    @PostMapping("/request")
    public ResponseEntity<BaseResponse<BusinessDto>> requestBusiness(@Valid @RequestBody BusinessRequest request) {
        return ResponseEntity.ok(BaseResponse.ok("Business request submitted successfully",
                businessService.requestBusiness(request)));
    }

    @PutMapping("/{businessId}/approve")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'approve-business')")
    @AuditedAction(value = ActivityEventType.BUSINESS_APPROVED, resourceType = "BUSINESS", resourceIdParam = "businessId")
    public ResponseEntity<BaseResponse<BusinessDto>> approveBusiness(@PathVariable Long businessId) {
        return ResponseEntity.ok(BaseResponse.ok("Business approved successfully",
                businessService.approveBusiness(businessId)));
    }

    @PutMapping("/{businessId}/status")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'approve-business')")
    public ResponseEntity<BaseResponse<BusinessDto>> updateBusinessStatus(
            @PathVariable Long businessId,
            @Valid @RequestBody UpdateBusinessStatusRequest request) {
        return ResponseEntity.ok(BaseResponse.ok("Business status updated successfully",
                businessService.updateBusinessStatus(businessId, request)));
    }

    @GetMapping("/{businessId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-business')")
    public ResponseEntity<BaseResponse<BusinessDto>> getBusinessById(@PathVariable Long businessId) {
        return ResponseEntity.ok(BaseResponse.ok("Business fetched successfully",
                businessService.getBusinessById(businessId)));
    }

    /**
     * DELETE /api/admin/businesses/{businessId}
     * Permanently deletes a REJECTED agency along with all associated data
     * (users, transactions, wallet deposits, notifications, role assignments, etc.).
     * Only agencies with status REJECTED can be deleted.
     */
    @DeleteMapping("/{businessId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-business')")
    public ResponseEntity<BaseResponse<Void>> deleteRejectedAgency(@PathVariable Long businessId) {
        businessService.deleteRejectedAgency(businessId);
        return ResponseEntity.ok(BaseResponse.<Void>ok(
                "Agency and all associated data deleted successfully"));
    }

    // ── provider management endpoints ───────────────────────────────────────

    /**
     * GET /api/admin/businesses/{businessId}/providers
     * Returns all providers assigned to the agency.
     */
    @GetMapping("/{businessId}/providers")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-business')")
    public ResponseEntity<BaseResponse<List<Provider>>> getBusinessProviders(@PathVariable Long businessId) {
        List<Provider> providers = businessProviderService.getProviders(businessId);
        return ResponseEntity.ok(BaseResponse.ok("Providers fetched successfully", providers));
    }

    /**
     * PUT /api/admin/businesses/{businessId}/providers
     * Replace the full provider list for an agency.
     * Body: ["TBO", "SABRE", "VERTEIL"]
     */
    @PutMapping("/{businessId}/providers")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-business')")
    public ResponseEntity<BaseResponse<List<Provider>>> setBusinessProviders(
            @PathVariable Long businessId,
            @Valid @RequestBody List<Provider> providers) {
        businessProviderService.setProviders(businessId, providers);
        return ResponseEntity.ok(BaseResponse.ok("Providers updated successfully",
                businessProviderService.getProviders(businessId)));
    }

    /**
     * POST /api/admin/businesses/{businessId}/providers/{provider}
     * Add a single provider to the agency.
     */
    @PostMapping("/{businessId}/providers/{provider}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-business')")
    public ResponseEntity<BaseResponse<List<Provider>>> addBusinessProvider(
            @PathVariable Long businessId,
            @PathVariable Provider provider) {
        businessProviderService.addProvider(businessId, provider);
        return ResponseEntity.ok(BaseResponse.ok("Provider added successfully",
                businessProviderService.getProviders(businessId)));
    }

    /**
     * DELETE /api/admin/businesses/{businessId}/providers/{provider}
     * Remove a single provider from the agency.
     */
    @DeleteMapping("/{businessId}/providers/{provider}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-business')")
    public ResponseEntity<BaseResponse<List<Provider>>> removeBusinessProvider(
            @PathVariable Long businessId,
            @PathVariable Provider provider) {
        businessProviderService.removeProvider(businessId, provider);
        return ResponseEntity.ok(BaseResponse.ok("Provider removed successfully",
                businessProviderService.getProviders(businessId)));
    }

    // ── sales-person management endpoints ───────────────────────────────────

    /**
     * GET /api/admin/businesses/{businessId}/sales-persons
     * Returns all sales persons assigned to the business.
     */
    @GetMapping("/{businessId}/sales-persons")
    @PreAuthorize("@permissionService.isFullAdmin(authentication)")
    public ResponseEntity<BaseResponse<List<SalesPersonResponseDto>>> getBusinessSalesPersons(
            @PathVariable Long businessId) {
        return ResponseEntity.ok(BaseResponse.ok("Sales persons fetched successfully",
                businessSalesPersonService.getSalesPersons(businessId)));
    }

    /**
     * PUT /api/admin/businesses/{businessId}/sales-persons
     * Replace the full sales-person list for a business.
     * Body: [1, 2, 3]
     */
    @PutMapping("/{businessId}/sales-persons")
    @PreAuthorize("@permissionService.isFullAdmin(authentication)")
    public ResponseEntity<BaseResponse<List<SalesPersonResponseDto>>> setBusinessSalesPersons(
            @PathVariable Long businessId,
            @RequestBody List<Long> salesPersonIds) {
        businessSalesPersonService.setSalesPersons(businessId, salesPersonIds);
        return ResponseEntity.ok(BaseResponse.ok("Sales persons updated successfully",
                businessSalesPersonService.getSalesPersons(businessId)));
    }

    /**
     * POST /api/admin/businesses/{businessId}/sales-persons/{salesPersonId}
     * Assign a single sales person to the business.
     */
    @PostMapping("/{businessId}/sales-persons/{salesPersonId}")
    @PreAuthorize("@permissionService.isFullAdmin(authentication)")
    public ResponseEntity<BaseResponse<List<SalesPersonResponseDto>>> addBusinessSalesPerson(
            @PathVariable Long businessId,
            @PathVariable Long salesPersonId) {
        businessSalesPersonService.addSalesPerson(businessId, salesPersonId);
        return ResponseEntity.ok(BaseResponse.ok("Sales person added successfully",
                businessSalesPersonService.getSalesPersons(businessId)));
    }

    /**
     * DELETE /api/admin/businesses/{businessId}/sales-persons/{salesPersonId}
     * Remove a single sales person from the business.
     */
    @DeleteMapping("/{businessId}/sales-persons/{salesPersonId}")
    @PreAuthorize("@permissionService.isFullAdmin(authentication)")
    public ResponseEntity<BaseResponse<List<SalesPersonResponseDto>>> removeBusinessSalesPerson(
            @PathVariable Long businessId,
            @PathVariable Long salesPersonId) {
        businessSalesPersonService.removeSalesPerson(businessId, salesPersonId);
        return ResponseEntity.ok(BaseResponse.ok("Sales person removed successfully",
                businessSalesPersonService.getSalesPersons(businessId)));
    }
}

