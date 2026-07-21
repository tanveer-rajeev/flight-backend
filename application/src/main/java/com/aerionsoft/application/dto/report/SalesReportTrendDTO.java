package com.aerionsoft.application.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportTrendDTO {
    private String date; // e.g. "YYYY-MM-DD"
    private Long totalTickets;
    private Double revenue;
}
