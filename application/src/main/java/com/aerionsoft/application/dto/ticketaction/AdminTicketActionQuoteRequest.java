package com.aerionsoft.application.dto.ticketaction;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class AdminTicketActionQuoteRequest {

    @NotNull
    private BigDecimal airlineCost;

    @NotNull
    private BigDecimal serviceCharge;

    @NotNull
    private BigDecimal totalAmount;

    private String currency;

    private String details;

    private String adminNote;

    /**
     * User must confirm the quote before this deadline.
     * If the deadline passes while status is QUOTED, the request becomes REJECTED automatically.
     */
    private LocalDateTime acceptDeadline;

    /**
     * Informational timeline for the customer.
     * Examples: "3 days", "3 weeks".
     */
    private String refundTimeline;
}
