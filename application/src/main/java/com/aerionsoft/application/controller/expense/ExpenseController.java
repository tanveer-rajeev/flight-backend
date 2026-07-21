package com.aerionsoft.application.controller.expense;

import com.aerionsoft.application.controller.BaseController;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.expense.ExpenseRequest;
import com.aerionsoft.application.dto.expense.ExpenseResponse;
import com.aerionsoft.application.enums.expense.ExpenseStatus;
import com.aerionsoft.application.enums.common.UsingPortal;
import com.aerionsoft.application.service.expense.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/expense")
public class ExpenseController extends BaseController{

    @Autowired
    private ExpenseService expenseService;

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-expense')") // admin and user
    public ResponseEntity<BaseResponse<ExpenseResponse>> createExpense(
            @Valid @RequestBody ExpenseRequest request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication();
        ExpenseResponse response = expenseService.createExpense(request, userId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Expense created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-expense')") // admin and user
    public ResponseEntity<BaseResponse<ExpenseResponse>> getExpenseById(@PathVariable Long id) {
        Long userId = getUserIdFromAuthentication();
        boolean isAdmin = isAdmin();
        ExpenseResponse response = expenseService.getExpenseById(id, userId, isAdmin);
        return ResponseEntity.ok(BaseResponse.ok(response, "Expense retrieved successfully"));
    }

    @GetMapping("/all")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-expense')") // admin and user
    public ResponseEntity<BaseResponse<List<ExpenseResponse>>> getAllExpenses() {
        Long userId = getUserIdFromAuthentication();
        boolean isAdmin = isAdmin();
        List<ExpenseResponse> response = expenseService.getAllExpenses(userId, isAdmin);
        return ResponseEntity.ok(BaseResponse.ok(response, "Expenses retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-expense')") // admin and user
    public ResponseEntity<BaseResponse<Page<ExpenseResponse>>> getAllExpensesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Long userId = getUserIdFromAuthentication();
        boolean isAdmin = isAdmin();
        Page<ExpenseResponse> response = expenseService.getAllExpensesPaginated(page, size, sortBy, sortDir, userId, isAdmin);
        return ResponseEntity.ok(BaseResponse.ok(response, "Expenses retrieved successfully with pagination"));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-expense')") // admin and user
    public ResponseEntity<BaseResponse<List<ExpenseResponse>>> getExpensesByStatus(
            @PathVariable ExpenseStatus status) {
        List<ExpenseResponse> response = expenseService.getExpensesByStatus(status);
        return ResponseEntity.ok(BaseResponse.ok(response, "Expenses by status retrieved successfully"));
    }

    @GetMapping("/created-by/{createdBy}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-expense')") // admin and user
    public ResponseEntity<BaseResponse<List<ExpenseResponse>>> getExpensesByCreatedBy(
            @PathVariable Long createdBy) {
        List<ExpenseResponse> response = expenseService.getExpensesByCreatedBy(createdBy);
        return ResponseEntity.ok(BaseResponse.ok(response, "Expenses by creator retrieved successfully"));
    }

    @GetMapping("/portal/{portal}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-expense')") // admin and user
    public ResponseEntity<BaseResponse<List<ExpenseResponse>>> getExpensesByPortal(
            @PathVariable UsingPortal portal) {
        List<ExpenseResponse> response = expenseService.getExpensesByPortal(portal);
        return ResponseEntity.ok(BaseResponse.ok(response, "Expenses by portal retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-expense')") // admin and user
    public ResponseEntity<BaseResponse<ExpenseResponse>> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication();
        ExpenseResponse response = expenseService.updateExpense(id, request, userId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Expense updated successfully"));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'approve-expense')")
    public ResponseEntity<BaseResponse<ExpenseResponse>> approveExpense(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication();
        ExpenseResponse response = expenseService.approveExpense(id, userId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Expense approved successfully"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'reject-expense')")
    public ResponseEntity<BaseResponse<ExpenseResponse>> rejectExpense(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication();
        ExpenseResponse response = expenseService.rejectExpense(id, userId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Expense rejected successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-expense')")
    public ResponseEntity<BaseResponse<Void>> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.ok(BaseResponse.ok("Expense deleted successfully"));
    }

}
