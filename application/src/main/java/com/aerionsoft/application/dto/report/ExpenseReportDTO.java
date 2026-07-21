package com.aerionsoft.application.dto.report;

import com.aerionsoft.application.enums.expense.ExpenseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseReportDTO {
    private Long id;
    private String expenseTitle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal amount;
    private String expenseAttachment;
    private String expenseDescription;
    private ExpenseStatus ExpenseStatus;
    private Long createdBy;
    private Long updatedBy;
    private Long approvedBy;

    // JPQL-safe constructor
    public ExpenseReportDTO(
            Long id,
            String expenseTitle,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Number amount,
            String expenseAttachment,
            String expenseDescription,
            ExpenseStatus expenseStatus,
            Long createdBy,
            Long updatedBy
    ) {
        this.id = id;
        this.expenseTitle = expenseTitle;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.amount = amount == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(amount.doubleValue());
        this.expenseAttachment = expenseAttachment;
        this.expenseDescription = expenseDescription;
        this.ExpenseStatus = expenseStatus;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }
}
