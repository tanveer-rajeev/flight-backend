package com.aerionsoft.application.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierTransactionReportRowDTO {
    private LocalDateTime date;
    private String pnr;
    private String agency;
    private String paxName;
    private String route;
    private String flightDate;
    private String ticketNo;
    private BigDecimal purchase;
    private BigDecimal sell;
    private BigDecimal profitLoss;
    private BigDecimal depositAmount;
    private BigDecimal outstandingBalance;
}
