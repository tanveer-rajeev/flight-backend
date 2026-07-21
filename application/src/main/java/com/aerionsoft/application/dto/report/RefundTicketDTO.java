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
public class RefundTicketDTO {
    private Long bookingId;
    private String pnr;
    private String ticketNo;
    private String bookingReference;
    private String airline;
    private String airlineCode;
    private String agencyName;
    private String customerName;
    private Double bookingPrice;
    private Double refundedAmount;
    private String currency;
    private LocalDateTime bookingDate;
    private LocalDateTime refundedAt;
    private String status;
    private String route;
}

