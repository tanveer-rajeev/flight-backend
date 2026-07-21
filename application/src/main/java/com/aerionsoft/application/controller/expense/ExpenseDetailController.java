package com.aerionsoft.application.controller.expense;

import com.aerionsoft.application.controller.BaseController;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.expense.ExpenseDetailRequest;
import com.aerionsoft.application.dto.expense.ExpenseDetailResponse;
import com.aerionsoft.application.service.expense.ExpenseDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/expense-detail")
public class ExpenseDetailController  extends BaseController {

    @Autowired
    private ExpenseDetailService expenseDetailService;

    @PostMapping("/expense/{expenseId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-expense-detail')") // admin and user
    public ResponseEntity<BaseResponse<ExpenseDetailResponse>> createExpenseDetail(
            @PathVariable Long expenseId,
            @Valid @RequestBody ExpenseDetailRequest request,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        ExpenseDetailResponse response = expenseDetailService.createExpenseDetail(request, expenseId, userId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Expense Detail created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-expense-detail')") // admin and user
    public ResponseEntity<BaseResponse<ExpenseDetailResponse>> getExpenseDetailById(@PathVariable Long id) {
        Long userId = getUserIdFromAuthentication();
        boolean isAdmin = isAdmin();
        ExpenseDetailResponse response = expenseDetailService.getExpenseDetailById(id, userId, isAdmin);
        return ResponseEntity.ok(BaseResponse.ok(response, "Expense Detail retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-expense-detail')") // admin and user
    public ResponseEntity<BaseResponse<List<ExpenseDetailResponse>>> getAllExpenseDetails() {
        Long userId = getUserIdFromAuthentication();
        boolean isAdmin = isAdmin();
        List<ExpenseDetailResponse> response = expenseDetailService.getAllExpenseDetails(userId, isAdmin);
        return ResponseEntity.ok(BaseResponse.ok(response, "Expense Details retrieved successfully"));
    }

    @GetMapping("/expense/{expenseId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-expense-detail')") // admin and user
    public ResponseEntity<BaseResponse<List<ExpenseDetailResponse>>> getExpenseDetailsByExpenseId(
            @PathVariable Long expenseId) {
        List<ExpenseDetailResponse> response = expenseDetailService.getExpenseDetailsByExpenseId(expenseId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Expense Details by expense retrieved successfully"));
    }

    @GetMapping("/account-head/{accountHeadId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-expense-detail')") // admin and user
    public ResponseEntity<BaseResponse<List<ExpenseDetailResponse>>> getExpenseDetailsByAccountHeadId(
            @PathVariable Long accountHeadId) {
        List<ExpenseDetailResponse> response = expenseDetailService.getExpenseDetailsByAccountHeadId(accountHeadId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Expense Details by account head retrieved successfully"));
    }

    @GetMapping("/expense/{expenseId}/total")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-expense-detail')") // admin and user
    public ResponseEntity<BaseResponse<BigDecimal>> calculateTotalByExpenseId(
            @PathVariable Long expenseId) {
        BigDecimal total = expenseDetailService.calculateTotalByExpenseId(expenseId);
        return ResponseEntity.ok(BaseResponse.ok(total, "Total calculated successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-expense-detail')") // admin and user
    public ResponseEntity<BaseResponse<ExpenseDetailResponse>> updateExpenseDetail(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseDetailRequest request,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        ExpenseDetailResponse response = expenseDetailService.updateExpenseDetail(id, request, userId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Expense Detail updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-expense-detail')")
    public ResponseEntity<BaseResponse<Void>> deleteExpenseDetail(@PathVariable Long id) {
        expenseDetailService.deleteExpenseDetail(id);
        return ResponseEntity.ok(BaseResponse.ok("Expense Detail deleted successfully"));
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        // This is a placeholder - adjust based on your actual authentication implementation
        return getUserIdFromAuthentication(); // Replace with actual user ID extraction logic
    }
}
