package com.aerionsoft.application.dto.ledger;

import com.aerionsoft.application.enums.wallet.CreditLimitStatus;
import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.enums.wallet.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerResponse {
    private Long transactionId;
    private String type;
    private Double amount;
    private String currency;
    private Double exchangeRate;
    private Double convertedAmount;
    private String description;
    private LocalDateTime createdAt;
    private String createdBy;
    private String agentCode;
    private String agentName;
    private TransactionStatus transactionStatus;


    // Reference info
    private String reference;

    private String sourceType;
    private Long sourceId;
    private SourceSummary sourceSummary;

    // For deposits
    private DepositInfo depositInfo;

    // For deductions (booking)
    private BookingInfo bookingInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SourceSummary {
        private String label;
        private String detail;
        private String status;
    }

    // Running balance (optional)
    private Double runningBalance;

    // For credit limit history
    private CreditLimitInfo creditLimitInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreditLimitInfo {
        private Long creditLimitHistoryId;
        private Long businessId;
        private String businessName;
        private BigDecimal amount;
        private String cause;
        private LocalDateTime returnDate;
        private String adminInstruction;
        private CreditLimitStatus creditLimitStatus;
        private BigDecimal balanceBefore;
        private BigDecimal balanceAfter;
        private String createdByName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DepositInfo {
        private Long depositId;
        private String depositType;
        private DepositStatus depositStatus;
        private String depositReference;
        private String remarks;
        private String depositBank;
        private Double exchangeRate;
        private Double exchangedAmount;
        private String depositCurrency;
        private LocalDateTime approvedAt;
        private String approvedBy;
        private String createdBy;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingInfo {
        private Long bookingId;
        private String pnr;
        private String ticketNo;
        private String airline;
        private String bookingStatus;
        private String bookingClass;
        private String providerName;
        private String customer;
        private LocalDateTime bookingDate;
        private String channel;
        private String bookingRef;
        private String airlineCode;
        private String route;
        private String flightDate;
        private String paxName;
    }
}

