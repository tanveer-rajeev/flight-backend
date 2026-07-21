package com.aerionsoft.application.dto.client.invoice.response;

import com.aerionsoft.application.dto.report.SupplierTransactionReportRowDTO;
import com.aerionsoft.application.dto.report.SupplierTransactionReportSummaryDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupplierTransactionHistoryWithTotalDTO {
    private BigDecimal totalPayable;
    private BigDecimal totalPaidAmount;
    private SupplierTransactionReportSummaryDTO summary;
    private Page<SupplierTransactionReportRowDTO> supplierTransactionHistoryWithTotalDTO;
}
