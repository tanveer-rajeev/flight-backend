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
public class AdminSalesReportDTO {
    private Long totalTickets;
    private Double totalRevenue;   // sum of profit/loss (sellPrice - buyPrice)
    private Double totalTax;
    private Double netRevenue;     // totalRevenue - totalTax
    private Double revenueAvg;     // totalRevenue / totalTickets
    private List<SalesReportTrendDTO> salesTrend;
    private Page<ConfirmedTicketDTO> confirmedTickets;
}
