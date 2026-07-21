package com.aerionsoft.application.dto.credit;

import com.aerionsoft.application.enums.wallet.CreditLimitStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditLimitRequest {

    private Long businessId;

    private BigDecimal amount;

    private String cause;

    private LocalDateTime returnDate;

    private String adminInstruction;

    private CreditLimitStatus status; // CREDIT or DEBIT
}

