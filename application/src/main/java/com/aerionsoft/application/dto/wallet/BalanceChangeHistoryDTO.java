package com.aerionsoft.application.dto.wallet;

import com.aerionsoft.application.enums.booking.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceChangeHistoryDTO {

    private Long   id;
    private Long   userId;

    /** CREDIT or DEBIT */
    private String changeType;

    private Double amount;
    private Double balanceBefore;
    private Double balanceAfter;

    /** E.g. provider name, "REFUND", "DEPOSIT", "MANUAL" */
    private String reason;

    /** Service that triggered the change, e.g. "BookingService" */
    private String source;

    /** Booking ID, deposit ID, etc. */
    private Long   referenceId;

    /** "BOOKING", "DEPOSIT", "MANUAL" */
    private String referenceType;

    /** userId of the actor (admin / agent / system) */
    private Long   performedBy;

    private LocalDateTime createdAt;

    /** Populated when referenceType == "BOOKING" */
    private TicketInfo ticket;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TicketInfo {
        private Long          bookingId;
        private String        bookingReference;
        private String        pnr;
        private String        ticketNo;
        private String        airline;
        private String        airlinePnrs;
        private BookingStatus status;
    }
}
