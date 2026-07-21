package com.aerionsoft.application.dto.expense;

import com.aerionsoft.application.dto.client.invoice.response.LedgerShortDTO;
import com.aerionsoft.application.enums.expense.ExpenseStatus;
import com.aerionsoft.application.enums.common.UsingPortal;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponse {

    private Long id;
    private LedgerShortDTO ledger;
    private String expenseTitle;
    private String expenseDescription;
    private BigDecimal expenseAmount;
    private ExpenseStatus expenseStatus;
    private String expenseAttachment;
    private Long approvedBy;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UsingPortal usingPortal;
    private Long portalId;
    private List<ExpenseDetailResponse> expenseDetails;
}

