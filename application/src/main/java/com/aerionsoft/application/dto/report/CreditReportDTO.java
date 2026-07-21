package com.aerionsoft.application.dto.report;

import com.aerionsoft.application.dto.credit.CreditLimitHistoryResponse;
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
public class CreditReportDTO {

    private BigDecimal totalAmount;
    private long totalCount;
    private String currency;
    private Page<CreditLimitHistoryResponse> records;
}