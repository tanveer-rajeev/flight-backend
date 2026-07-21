package com.aerionsoft.application.controller.client;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.client.invoice.SupplierBulkAssignBranchRequest;
import com.aerionsoft.application.dto.client.invoice.SupplierProviderMappingDto;
import com.aerionsoft.application.dto.client.invoice.SupplierDto;
import com.aerionsoft.application.dto.client.invoice.response.SupplierBulkAssignBranchResponse;
import com.aerionsoft.application.dto.client.invoice.response.SupplierResponseDto;
import com.aerionsoft.application.service.client.SupplierService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/suppliers")
public class SupplerController extends BaseController {

    private final SupplierService supplierService;

    public SupplerController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    /**
     * Supplier list
     *
     * @param authentication authenticate user
     * @return response as JSON
     */
    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-supplier')")
    public ResponseEntity<BaseResponse<List<SupplierResponseDto>>> getSuppliers(
            @RequestParam(required = false) Long branchId,
            Authentication authentication
    ) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        List<SupplierResponseDto> suppliers = supplierService.getAllSuppliers(provider, authUserId, branchId);

        return ResponseEntity.ok(BaseResponse.ok(suppliers));
    }

    /**
     * Supplier show
     *
     * @param id             to find specific id
     * @param authentication authenticate user
     * @return response as JSON
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-supplier')")
    public ResponseEntity<BaseResponse<SupplierResponseDto>> getSupplierById(@PathVariable Long id, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        SupplierResponseDto supplier = supplierService.getSupplierById(provider, authUserId, id);

        return ResponseEntity.ok(BaseResponse.ok(supplier));
    }

    @GetMapping("/platform-providers")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-supplier')")
    public ResponseEntity<BaseResponse<List<SupplierProviderMappingDto>>> getPlatformProviders(
            Authentication authentication
    ) {
        return ResponseEntity.ok(BaseResponse.ok(supplierService.getPlatformProviderOptions()));
    }

    /**
     * Supplier Store
     *
     * @param supplierDto request to create Supplier
     * @return response as JSON
     */
    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-supplier')")
    public ResponseEntity<BaseResponse<?>> addSupplier(@Valid @RequestBody SupplierDto supplierDto, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        supplierService.createSupplier(provider, authUserId, supplierDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.created("Supplier created successfully", null));
    }

    /**
     * Bulk assign branch to multiple suppliers (e.g. migrate existing suppliers).
     */
    @PatchMapping("/bulk-assign-branch")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-supplier')")
    public ResponseEntity<BaseResponse<SupplierBulkAssignBranchResponse>> bulkAssignBranch(
            @Valid @RequestBody SupplierBulkAssignBranchRequest request,
            Authentication authentication
    ) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        SupplierBulkAssignBranchResponse result = supplierService.bulkAssignBranch(provider, authUserId, request);
        return ResponseEntity.ok(BaseResponse.ok(result, "Suppliers assigned to branch successfully"));
    }

    /**
     * Supplier Update
     *
     * @param id          to update specific Supplier
     * @param supplierDto request to update Supplier
     * @return response as JSON
     */
    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-supplier')")
    public ResponseEntity<BaseResponse<String>> updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierDto supplierDto, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        supplierService.updateSupplier(provider, authUserId, id, supplierDto);

        return ResponseEntity.ok(BaseResponse.ok("Supplier updated successfully"));
    }

    /**
     * Delete supplier
     *
     * @param id             specific id to delete
     * @param authentication authenticate user
     * @return response as JSON
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-supplier')")
    public ResponseEntity<BaseResponse<String>> deleteSupplier(@PathVariable Long id, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        supplierService.deleteSupplier(provider, authUserId, id);

        return ResponseEntity.ok(BaseResponse.ok("Supplier deleted successfully"));
    }

}
