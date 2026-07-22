package com.aerionsoft.application.dto.ticketaction;

import com.aerionsoft.application.enums.booking.TicketActionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class AdminTicketActionFinalizeRequest {

    @NotNull
    private TicketActionStatus resultStatus; // COMPLETED or FAILED

    private String finalResult;

    private String externalReference;

    /**
     * Required when {@code resultStatus = COMPLETED}.
     * Amount the supplier keeps (USD). Remaining payable for the PNR = this value.
     * Reversal from supplier payable = booking buyPrice - supplierRefundCost.
     * Use {@code 0} when the supplier credits the full buy price.
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "supplierRefundCost must be zero or greater")
    private BigDecimal supplierRefundCost;

    /**
     * Required when completing a {@code REISSUE} ticket action ({@code resultStatus = COMPLETED}).
     * Calendar date the ticket was reissued with the airline.
     */
    private LocalDate reissueDate;
}
