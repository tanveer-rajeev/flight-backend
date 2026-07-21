package com.aerionsoft.application.dto.expense;

import com.aerionsoft.application.enums.expense.ExpenseStatus;
import com.aerionsoft.application.enums.common.UsingPortal;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseRequest {
    private Long ledgerId;
    private String expenseTitle;
    private String expenseDescription;
    private ExpenseStatus expenseStatus;
    private String expenseAttachment;
    private UsingPortal usingPortal;
    private Long portalId;
    private List<ExpenseDetailRequest> expenseDetails;
}

