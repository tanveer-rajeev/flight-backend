package com.aerionsoft.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DailyReportResponseDTO {
    private int totalBooking;
    private int totalBookingProcessed;
    private int totalBookingPnr;
    private int totalBookingConfirmed;
    private int totalBookingCanceled;
    private int totalBookingCBooked;
    private int totalBookingTicketed;
    private int totalBookingOnHold;
    private int totalBookingVoided;
    private int totalBookingTicketIssued;

    private BigDecimal totalPendingDepositBdt;
    private BigDecimal totalPendingDepositInr;
    private BigDecimal totalPendingDepositUsd;
    private BigDecimal totalPendingDepositPkr;
    private BigDecimal totalPendingDepositSar;
    private BigDecimal totalPendingDepositQar;

    private BigDecimal totalApprovedDepositBdt;
    private BigDecimal totalApprovedDepositInr;
    private BigDecimal totalApprovedDepositUsd;
    private BigDecimal totalApprovedDepositPkr;
    private BigDecimal totalApprovedDepositSar;
    private BigDecimal totalApprovedDepositQar;

    private int totalUserCreated;
    private int totalAgencyCreated;
}



