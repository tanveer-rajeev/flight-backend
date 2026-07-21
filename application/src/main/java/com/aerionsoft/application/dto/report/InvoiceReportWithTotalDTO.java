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
public class InvoiceReportWithTotalDTO {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;

    private Page<InvoiceReportDTO> invoiceReport;
}
