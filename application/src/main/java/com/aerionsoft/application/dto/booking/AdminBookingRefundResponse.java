package com.aerionsoft.application.dto.booking;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AdminBookingRefundResponse {

    private Long bookingId;
    private String pnr;
    private String ticketNo;

    /** Original booking price in system currency (USD) */
    private BigDecimal bookingPrice;

    /** Amount deducted as fee/penalty (0 for FULL refund) */
    private BigDecimal deductionAmount;

    /** Amount actually credited back to user wallet */
    private BigDecimal refundedAmount;

    /** Supplier buy price (USD) used for payable adjustment */
    private BigDecimal buyPrice;

    /**
     * Supplier-side cost kept on refund (USD).
     * Remaining payable against this PNR = supplierRefundCost.
     */
    private BigDecimal supplierRefundCost;

    /** Amount reversed from supplier payable (USD) = buyPrice - supplierRefundCost */
    private BigDecimal supplierPayableReversed;

    /** Remaining supplier payable for this booking (USD) = supplierRefundCost */
    private BigDecimal remainingSupplierPayable;

    /** Original booking margin (USD) = bookingPrice - buyPrice */
    private BigDecimal profitLoss;

    /**
     * Margin after refund (USD) = profitLoss - supplierRefundCost + deductionAmount.
     * Positive = agency still ahead vs buy price after supplier cost and customer fee kept.
     */
    private BigDecimal netProfitLoss;

    private String refundType;
    private String reason;

    /** User's display currency */
    private String currency;
}
