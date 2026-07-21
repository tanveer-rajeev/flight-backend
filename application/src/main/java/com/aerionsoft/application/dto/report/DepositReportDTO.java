package com.aerionsoft.application.dto.report;

import com.aerionsoft.application.dto.admin.bank.WalletDepositResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositReportDTO {

    private BigDecimal totalAmount;
    private long totalCount;
    private String currency;
    private Page<WalletDepositResponse> records;
}

