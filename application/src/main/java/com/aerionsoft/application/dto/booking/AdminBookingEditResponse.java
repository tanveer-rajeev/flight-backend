package com.aerionsoft.application.dto.booking;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AdminBookingEditResponse {

    private Long bookingId;
    private String pnr;
    private String ticketNo;

    /** Stored sell/buy on booking (USD). */
    private BigDecimal bookingPrice;
    private BigDecimal buyPrice;
    private BigDecimal profitLoss;

    /** Agency currency used to interpret request sell/buy inputs. */
    private String inputCurrency;
    /** Request sell/buy as submitted (agency currency), when provided. */
    private BigDecimal bookingPriceInput;
    private BigDecimal buyPriceInput;

    private Long ownerUserId;
    private String ownerName;
    private String reason;

    /** Field → { before, after } for audit/UI. */
    private Map<String, FieldChange> changes;

    /** Wallet deltas in the charged/credited agency currency. */
    private BigDecimal walletDeltaCharged;
    private BigDecimal walletDeltaCredited;
    private Long transferredFromUserId;
    private Long transferredToUserId;
    private List<TravellerChange> travellerChanges;

    @Getter
    @Builder
    public static class FieldChange {
        private Object before;
        private Object after;
    }

    @Getter
    @Builder
    public static class TravellerChange {
        private Long travellerId;
        private String beforeName;
        private String afterName;
    }
}
