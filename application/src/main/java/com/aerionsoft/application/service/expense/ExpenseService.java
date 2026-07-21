package com.aerionsoft.application.service.expense;

import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.client.invoice.response.LedgerShortDTO;
import com.aerionsoft.application.dto.expense.ExpenseDetailRequest;
import com.aerionsoft.application.dto.expense.ExpenseDetailResponse;
import com.aerionsoft.application.dto.expense.ExpenseRequest;
import com.aerionsoft.application.dto.expense.ExpenseResponse;
import com.aerionsoft.application.entity.client.Ledger;
import com.aerionsoft.application.entity.expense.Expense;
import com.aerionsoft.application.enums.expense.ExpenseStatus;
import com.aerionsoft.application.enums.common.UsingPortal;
import com.aerionsoft.application.repository.expense.ExpenseDetailRepository;
import com.aerionsoft.application.repository.expense.ExpenseRepository;
import com.aerionsoft.application.repository.client.InvoiceLedgerRepository;
import com.aerionsoft.application.util.TimestampMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseDetailRepository expenseDetailRepository;

    @Autowired
    private ExpenseDetailService expenseDetailService;

    @Autowired
    private InvoiceLedgerRepository ledgerRepository;

    @Autowired
    private TimestampMapper timestampMapper;

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request, Long userId) {

        Ledger ledger = ledgerRepository.findById(request.getLedgerId()).orElseThrow(() -> new ResourceNotFoundException("Ledger"));

        Expense expense = Expense.builder()
                .ledger(ledger)
                .expenseTitle(request.getExpenseTitle())
                .expenseDescription(request.getExpenseDescription())
                .expenseStatus(request.getExpenseStatus() != null ? request.getExpenseStatus() : ExpenseStatus.PENDING)
                .expenseAttachment(request.getExpenseAttachment())
                .expenseAmount(BigDecimal.ZERO)
                .usingPortal(request.getUsingPortal())
                .portalId(request.getPortalId())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        Expense savedExpense = expenseRepository.save(expense);

        // Create expense details if provided
        List<ExpenseDetailResponse> detailResponses = new ArrayList<>();
        if (request.getExpenseDetails() != null && !request.getExpenseDetails().isEmpty()) {
            for (ExpenseDetailRequest detailRequest : request.getExpenseDetails()) {
                ExpenseDetailResponse detailResponse = expenseDetailService.createExpenseDetail(
                        detailRequest, savedExpense.getId(), userId);
                detailResponses.add(detailResponse);
            }

            // Calculate and update total expense amount
            BigDecimal totalAmount = expenseDetailService.calculateTotalByExpenseId(savedExpense.getId());
            savedExpense.setExpenseAmount(totalAmount);
            savedExpense = expenseRepository.save(savedExpense);
        }

        return mapToResponse(savedExpense, detailResponses);
    }

    @Transactional
    public ExpenseResponse getExpenseById(Long id, Long userId, boolean isAdmin) {
        Expense expense;

        if (isAdmin) {
            // Admin users can access records with usingPortal = ADMIN
            expense = expenseRepository.findByIdAndUsingPortal(id, UsingPortal.ADMIN)
                    .orElseThrow(() -> new ResourceNotFoundException("Expense", id + " or access denied"));
        } else {
            // Regular users can only access their own records
            expense = expenseRepository.findByIdAndCreatedBy(id, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Expense", id + " or access denied"));
        }

        List<ExpenseDetailResponse> details = expenseDetailService.getExpenseDetailsByExpenseId(id);
        return mapToResponse(expense, details);
    }
    @Transactional
    public List<ExpenseResponse> getAllExpenses(Long userId, boolean isAdmin) {
        List<Expense> expenses;

        if (isAdmin) {
            // Admin users see all records with usingPortal = ADMIN
            expenses = expenseRepository.findByUsingPortal(UsingPortal.ADMIN);
        } else {
            // Regular users see only their own records
            expenses = expenseRepository.findByCreatedBy(userId);
        }

        return expenses.stream()
                .map(expense -> {
                    List<ExpenseDetailResponse> details = expenseDetailService.getExpenseDetailsByExpenseId(expense.getId());
                    return mapToResponse(expense, details);
                })
                .collect(Collectors.toList());
    }

    public Page<ExpenseResponse> getAllExpensesPaginated(int page, int size, String sortBy, String sortDir, Long userId, boolean isAdmin) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Expense> expensePage;
        if (isAdmin) {
            // Admin users see all records with usingPortal = ADMIN
            expensePage = expenseRepository.findByUsingPortal(UsingPortal.ADMIN, pageable);
        } else {
            // Regular users see only their own records
            expensePage = expenseRepository.findByCreatedBy(userId, pageable);
        }

        return expensePage.map(expense -> {
            List<ExpenseDetailResponse> details = expenseDetailService.getExpenseDetailsByExpenseId(expense.getId());
            return mapToResponse(expense, details);
        });
    }

    public List<ExpenseResponse> getExpensesByStatus(ExpenseStatus status) {
        return expenseRepository.findByExpenseStatus(status).stream()
                .map(expense -> {
                    List<ExpenseDetailResponse> details = expenseDetailService.getExpenseDetailsByExpenseId(expense.getId());
                    return mapToResponse(expense, details);
                })
                .collect(Collectors.toList());
    }

    public List<ExpenseResponse> getExpensesByCreatedBy(Long createdBy) {
        return expenseRepository.findByCreatedBy(createdBy).stream()
                .map(expense -> {
                    List<ExpenseDetailResponse> details = expenseDetailService.getExpenseDetailsByExpenseId(expense.getId());
                    return mapToResponse(expense, details);
                })
                .collect(Collectors.toList());
    }

    public List<ExpenseResponse> getExpensesByPortal(UsingPortal usingPortal) {
        return expenseRepository.findByUsingPortal(usingPortal).stream()
                .map(expense -> {
                    List<ExpenseDetailResponse> details = expenseDetailService.getExpenseDetailsByExpenseId(expense.getId());
                    return mapToResponse(expense, details);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request, Long userId) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));

        if (request.getExpenseTitle() != null) {
            expense.setExpenseTitle(request.getExpenseTitle());
        }
        if (request.getExpenseDescription() != null) {
            expense.setExpenseDescription(request.getExpenseDescription());
        }
        if (request.getExpenseStatus() != null) {
            expense.setExpenseStatus(request.getExpenseStatus());
        }
        if (request.getExpenseAttachment() != null) {
            expense.setExpenseAttachment(request.getExpenseAttachment());
        }
        if (request.getUsingPortal() != null) {
            expense.setUsingPortal(request.getUsingPortal());
        }
        if (request.getPortalId() != null) {
            expense.setPortalId(request.getPortalId());
        }

        expense.setUpdatedBy(userId);

        // Update expense details if provided
        if (request.getExpenseDetails() != null) {
            // Delete existing details
            expenseDetailRepository.deleteByExpenseId(id);

            // Create new details
            for (ExpenseDetailRequest detailRequest : request.getExpenseDetails()) {
                expenseDetailService.createExpenseDetail(detailRequest, id, userId);
            }

            // Recalculate total
            BigDecimal totalAmount = expenseDetailService.calculateTotalByExpenseId(id);
            expense.setExpenseAmount(totalAmount);
        }

        Expense updatedExpense = expenseRepository.save(expense);
        List<ExpenseDetailResponse> details = expenseDetailService.getExpenseDetailsByExpenseId(id);

        return mapToResponse(updatedExpense, details);
    }

    @Transactional
    public ExpenseResponse approveExpense(Long id, Long approvedBy) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));

        expense.setExpenseStatus(ExpenseStatus.APPROVED);
        expense.setApprovedBy(approvedBy);
        expense.setUpdatedBy(approvedBy);

        Expense updatedExpense = expenseRepository.save(expense);
        List<ExpenseDetailResponse> details = expenseDetailService.getExpenseDetailsByExpenseId(id);

        return mapToResponse(updatedExpense, details);
    }

    @Transactional
    public ExpenseResponse rejectExpense(Long id, Long rejectedBy) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));

        expense.setExpenseStatus(ExpenseStatus.REJECTED);
        expense.setApprovedBy(rejectedBy);
        expense.setUpdatedBy(rejectedBy);

        Expense updatedExpense = expenseRepository.save(expense);
        List<ExpenseDetailResponse> details = expenseDetailService.getExpenseDetailsByExpenseId(id);

        return mapToResponse(updatedExpense, details);
    }

    @Transactional
    public void deleteExpense(Long id) {
        if (!expenseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Expense", id);
        }

        // Delete associated expense details
        expenseDetailRepository.deleteByExpenseId(id);

        // Delete expense
        expenseRepository.deleteById(id);
    }

    private ExpenseResponse mapToResponse(Expense expense, List<ExpenseDetailResponse> details) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .ledger(expense.getLedger() != null ? new LedgerShortDTO(expense.getLedger().getId(), expense.getLedger().getTitle()) : null)
                .expenseTitle(expense.getExpenseTitle())
                .expenseDescription(expense.getExpenseDescription())
                .expenseAmount(expense.getExpenseAmount())
                .expenseStatus(expense.getExpenseStatus())
                .expenseAttachment(expense.getExpenseAttachment())
                .approvedBy(expense.getApprovedBy())
                .createdBy(expense.getCreatedBy())
                .updatedBy(expense.getUpdatedBy())
                .createdAt(timestampMapper.toRequestUserTime(expense.getCreatedAt(), expense.getCreatedTimeOffset()))
                .updatedAt(timestampMapper.toRequestUserTime(expense.getUpdatedAt(), expense.getUpdatedTimeOffset() != null ? expense.getUpdatedTimeOffset() : expense.getCreatedTimeOffset()))
                .usingPortal(expense.getUsingPortal())
                .portalId(expense.getPortalId())
                .expenseDetails(details)
                .build();
    }
}
