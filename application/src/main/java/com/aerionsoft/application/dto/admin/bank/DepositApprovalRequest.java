package com.aerionsoft.application.dto.admin.bank;

import com.aerionsoft.application.enums.wallet.DepositStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DepositApprovalRequest {
    @NotNull
    private DepositStatus status; // APPROVED or REJECTED
    private String adminRemarks;
}