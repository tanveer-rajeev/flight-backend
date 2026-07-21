package com.aerionsoft.application.dto.ticketaction;

import com.aerionsoft.application.enums.booking.TicketActionStatus;
import com.aerionsoft.application.enums.booking.TicketActionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TicketActionRequestResponse {
    private Long id;

    private Long bookingId;
    private String pnr;
    private String ticketNo;
    private String agencyName;
    private String bookingPrice;
    private String bookingPriceCurrency;

    private TicketActionType type;
    private TicketActionStatus status;

    private String reason;
    private String adminNote;

    private BigDecimal airlineCost;
    private BigDecimal serviceCharge;
    private BigDecimal totalAmount;
    private String currency;
    private String details;

    private BigDecimal quoteExchangeRate;
    private String quoteUserCurrency;

    private LocalDateTime createdAt;
    private LocalDateTime quotedAt;
    private LocalDateTime userConfirmedAt;
    private LocalDateTime finalizedAt;

    private String externalReference;
    private String finalResult;

    private LocalDateTime acceptDeadline;
    private String refundTimeline;

    private boolean refunded;

    /** Booking buy price (USD) at time of response */
    private BigDecimal buyPrice;

    /** Supplier cost kept — set after admin finalize COMPLETED */
    private BigDecimal supplierRefundCost;

    private BigDecimal supplierPayableReversed;

    private BigDecimal remainingSupplierPayable;

    /** Original booking margin (USD) = bookingPrice - buyPrice */
    private BigDecimal profitLoss;

    /** After refund: profitLoss - supplierRefundCost + quoteTotalAmount (customer fee kept) */
    private BigDecimal netProfitLoss;
}
