package com.aerionsoft.application.dto.audit;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ActivityFeedDetailsInfo {

    private Long bookingId;
    private String pnr;
    private String ticketNo;
    private String bookingReference;
    private Long ticketActionRequestId;
    private String ticketActionType;
    private String ticketActionStatus;
    private BigDecimal amount;
    private String currency;
    private String oldStatus;
    private String newStatus;
    private Long walletDepositId;
    private Long businessId;
}
