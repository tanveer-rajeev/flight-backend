package com.aerionsoft.application.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountHeadReportResponseWithTotalDTO {
    private BigDecimal totalExpense;
    private BigDecimal totalIncome;

    private Page<AccountHeadReportResponseDTO> accountHeadReportResponseDTO;
}
