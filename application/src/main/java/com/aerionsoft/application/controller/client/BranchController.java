package com.aerionsoft.application.controller.client;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.client.branch.BranchDto;
import com.aerionsoft.application.dto.client.branch.BranchResponseDto;
import com.aerionsoft.application.dto.client.invoice.response.SupplierResponseDto;
import com.aerionsoft.application.service.client.BranchService;
import com.aerionsoft.application.service.client.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/branches")
public class BranchController extends BaseController {

    private final BranchService branchService;
    private final SupplierService supplierService;

    public BranchController(BranchService branchService, SupplierService supplierService) {
        this.branchService = branchService;
        this.supplierService = supplierService;
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-branch')")
    public ResponseEntity<BaseResponse<List<BranchResponseDto>>> getBranches(
            @RequestParam(required = false) Boolean activeOnly,
            Authentication authentication
    ) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        List<BranchResponseDto> branches = branchService.getAllBranches(provider, authUserId, activeOnly);
        return ResponseEntity.ok(BaseResponse.ok(branches, "Branches retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-branch')")
    public ResponseEntity<BaseResponse<BranchResponseDto>> getBranchById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        BranchResponseDto branch = branchService.getBranchById(provider, authUserId, id);
        return ResponseEntity.ok(BaseResponse.ok(branch, "Branch retrieved successfully"));
    }

    @GetMapping("/{id}/suppliers")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-supplier')")
    public ResponseEntity<BaseResponse<List<SupplierResponseDto>>> getSuppliersByBranch(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        branchService.getBranchById(provider, authUserId, id);
        List<SupplierResponseDto> suppliers = supplierService.getSuppliersByBranchId(provider, authUserId, id);
        return ResponseEntity.ok(BaseResponse.ok(suppliers, "Suppliers retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-branch')")
    public ResponseEntity<BaseResponse<BranchResponseDto>> createBranch(
            @Valid @RequestBody BranchDto branchDto,
            Authentication authentication
    ) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        BranchResponseDto branch = branchService.createBranch(provider, authUserId, branchDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Branch created successfully", branch));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-branch')")
    public ResponseEntity<BaseResponse<BranchResponseDto>> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody BranchDto branchDto,
            Authentication authentication
    ) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        BranchResponseDto branch = branchService.updateBranch(provider, authUserId, id, branchDto);
        return ResponseEntity.ok(BaseResponse.ok(branch, "Branch updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-branch')")
    public ResponseEntity<BaseResponse<String>> deleteBranch(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        branchService.deleteBranch(provider, authUserId, id);
        return ResponseEntity.ok(BaseResponse.ok("Branch deleted successfully"));
    }
}
