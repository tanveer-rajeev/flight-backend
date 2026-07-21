package com.aerionsoft.application.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmedTicketDTO {
    private Long bookingId;
    private String pnr;
    private String ticketNo;
    private String airline;
    private String airlineCode;
    private String agencyName;
    private String customerName;
    private Double originalPrice;
    private Double buyPrice;
    private Double bookingPrice;
    /** Profit/loss = sell price − buy price (same value as {@link #profitLoss} for backward compatibility). */
    private Double markupAmount;
    private Double profitLoss;
    private Double totalFare;
    private Double tax;
    private String currency;
    private LocalDateTime bookingDate;
    private String status;
}
