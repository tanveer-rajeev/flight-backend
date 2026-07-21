package com.aerionsoft.application.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundReportDTO {
    private long totalRefunds;
    private Double totalRefundedAmount;
    private List<SalesReportTrendDTO> refundTrend;
    private Page<RefundTicketDTO> records;
}

