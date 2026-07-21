package com.aerionsoft.application.service.expense;

import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.expense.ExpenseDetailRequest;
import com.aerionsoft.application.dto.expense.ExpenseDetailResponse;
import com.aerionsoft.application.entity.AccountHead;
import com.aerionsoft.application.entity.expense.Expense;
import com.aerionsoft.application.entity.expense.ExpenseDetail;
import com.aerionsoft.application.enums.common.UsingPortal;
import com.aerionsoft.application.repository.finance.AccountHeadRepository;
import com.aerionsoft.application.repository.expense.ExpenseDetailRepository;
import com.aerionsoft.application.repository.expense.ExpenseRepository;
import com.aerionsoft.application.util.TimestampMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenseDetailService {

    @Autowired
    private ExpenseDetailRepository expenseDetailRepository;

    @Autowired
    private AccountHeadRepository accountHeadRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private TimestampMapper timestampMapper;

    @Transactional
    public ExpenseDetailResponse createExpenseDetail(ExpenseDetailRequest request, Long expenseId, Long userId) {
        ExpenseDetail expenseDetail = ExpenseDetail.builder()
                .expenseId(expenseId)
                .accountHeadId(request.getAccountHeadId())
                .itemTitle(request.getItemTitle())
                .itemDescription(request.getItemDescription())
                .itemAmount(request.getItemAmount())
                .itemAttachment(request.getItemAttachment())
                .usingPortal(request.getUsingPortal())
                .portalId(request.getPortalId())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        ExpenseDetail savedExpenseDetail = expenseDetailRepository.save(expenseDetail);

        // Update the total amount in the Expense entity by adding the new item amount to existing amount
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId));

        BigDecimal currentAmount = expense.getExpenseAmount() != null ? expense.getExpenseAmount() : BigDecimal.ZERO;
        BigDecimal newItemAmount = request.getItemAmount() != null ? request.getItemAmount() : BigDecimal.ZERO;
        expense.setExpenseAmount(currentAmount.add(newItemAmount));
        expenseRepository.save(expense);

        return mapToResponse(savedExpenseDetail);
    }

    public ExpenseDetailResponse getExpenseDetailById(Long id, Long userId, boolean isAdmin) {
        ExpenseDetail expenseDetail;

        if (isAdmin) {
            // Admin users can access records with usingPortal = ADMIN
            expenseDetail = expenseDetailRepository.findByIdAndUsingPortal(id, UsingPortal.ADMIN)
                    .orElseThrow(() -> new ResourceNotFoundException("Expense Detail", id + " or access denied"));
        } else {
            // Regular users can only access their own records
            expenseDetail = expenseDetailRepository.findByIdAndCreatedBy(id, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Expense Detail", id + " or access denied"));
        }

        return mapToResponse(expenseDetail);
    }

    public List<ExpenseDetailResponse> getAllExpenseDetails(Long userId, boolean isAdmin) {
        List<ExpenseDetail> expenseDetails;

        if (isAdmin) {
            // Admin users see all records with usingPortal = ADMIN
            expenseDetails = expenseDetailRepository.findByUsingPortal(UsingPortal.ADMIN);
        } else {
            // Regular users see only their own records
            expenseDetails = expenseDetailRepository.findByCreatedBy(userId);
        }

        return expenseDetails.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ExpenseDetailResponse> getExpenseDetailsByExpenseId(Long expenseId) {
        return expenseDetailRepository.findByExpenseId(expenseId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ExpenseDetailResponse> getExpenseDetailsByAccountHeadId(Long accountHeadId) {
        return expenseDetailRepository.findByAccountHeadId(accountHeadId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public BigDecimal calculateTotalByExpenseId(Long expenseId) {
        BigDecimal total = expenseDetailRepository.calculateTotalByExpenseId(expenseId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional
    public ExpenseDetailResponse updateExpenseDetail(Long id, ExpenseDetailRequest request, Long userId) {
        ExpenseDetail expenseDetail = expenseDetailRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense Detail", id));

        if (request.getAccountHeadId() != null) {
            expenseDetail.setAccountHeadId(request.getAccountHeadId());
        }
        if (request.getItemTitle() != null) {
            expenseDetail.setItemTitle(request.getItemTitle());
        }
        if (request.getItemDescription() != null) {
            expenseDetail.setItemDescription(request.getItemDescription());
        }
        if (request.getItemAmount() != null) {
            expenseDetail.setItemAmount(request.getItemAmount());
        }
        if (request.getItemAttachment() != null) {
            expenseDetail.setItemAttachment(request.getItemAttachment());
        }
        if (request.getUsingPortal() != null) {
            expenseDetail.setUsingPortal(request.getUsingPortal());
        }
        if (request.getPortalId() != null) {
            expenseDetail.setPortalId(request.getPortalId());
        }

        expenseDetail.setUpdatedBy(userId);

        ExpenseDetail updatedExpenseDetail = expenseDetailRepository.save(expenseDetail);
        return mapToResponse(updatedExpenseDetail);
    }

    @Transactional
    public void deleteExpenseDetail(Long id) {
        if (!expenseDetailRepository.existsById(id)) {
            throw new ResourceNotFoundException("Expense Detail", id);
        }
        expenseDetailRepository.deleteById(id);
    }

    private ExpenseDetailResponse mapToResponse(ExpenseDetail expenseDetail) {
        String accountHeadTitle = null;
        if (expenseDetail.getAccountHeadId() != null) {
            accountHeadTitle = accountHeadRepository.findById(expenseDetail.getAccountHeadId())
                    .map(AccountHead::getAccountHeadTitle)
                    .orElse(null);
        }

        return ExpenseDetailResponse.builder()
                .id(expenseDetail.getId())
                .expenseId(expenseDetail.getExpenseId())
                .accountHeadId(expenseDetail.getAccountHeadId())
                .accountHeadTitle(accountHeadTitle)
                .itemTitle(expenseDetail.getItemTitle())
                .itemDescription(expenseDetail.getItemDescription())
                .itemAmount(expenseDetail.getItemAmount())
                .itemAttachment(expenseDetail.getItemAttachment())
                .createdBy(expenseDetail.getCreatedBy())
                .updatedBy(expenseDetail.getUpdatedBy())
                .createdAt(timestampMapper.toRequestUserTime(expenseDetail.getCreatedAt(), expenseDetail.getCreatedTimeOffset()))
                .updatedAt(timestampMapper.toRequestUserTime(expenseDetail.getUpdatedAt(), expenseDetail.getUpdatedTimeOffset() != null ? expenseDetail.getUpdatedTimeOffset() : expenseDetail.getCreatedTimeOffset()))
                .usingPortal(expenseDetail.getUsingPortal())
                .portalId(expenseDetail.getPortalId())
                .build();
    }
}
