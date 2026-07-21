package com.aerionsoft.application.dto.booking;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBookingRefundRequest {

    public enum RefundType {
        FULL, PARTIAL
    }

    /**
     * FULL  – refund the entire booking price back to the user's wallet.
     * PARTIAL – deduct a fee (deductionAmount) and refund the remainder.
     */
    @NotNull(message = "Refund type is required (FULL or PARTIAL)")
    private RefundType refundType;

    /**
     * Required only when refundType = PARTIAL.
     * This is the amount that will be KEPT / deducted (e.g. airline penalty fee).
     * The user receives: bookingPrice - deductionAmount.
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "Deduction amount must be greater than 0")
    private BigDecimal deductionAmount;

    /**
     * Amount the supplier keeps on refund (cancellation penalty / non-refundable supplier cost).
     * Remaining supplier payable for this PNR = supplierRefundCost.
     * Reversed from supplier payable = buyPrice - supplierRefundCost.
     * Use 0 when the supplier credits the full buy price back.
     */
    @NotNull(message = "supplierRefundCost is required (use 0 when supplier refunds full buy price)")
    @DecimalMin(value = "0.0", inclusive = true, message = "supplierRefundCost must be zero or greater")
    private BigDecimal supplierRefundCost;

    @NotBlank(message = "Reason is required")
    private String reason;
}

