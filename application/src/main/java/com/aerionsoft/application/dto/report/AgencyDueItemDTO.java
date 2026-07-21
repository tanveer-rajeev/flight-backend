package com.aerionsoft.application.dto.report;

import com.aerionsoft.application.dto.client.user.TransactionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyDueItemDTO {

    private Long businessId;
    private String companyName;
    private String companyEmail;
    private String companyPhone;

    /** Negative balance from the mother user account — the amount this agency owes */
    private Double balance;

    /** Configured credit limit for this agency (from BusinessEntity) */
    private BigDecimal creditLimit;

    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * DEBIT transactions (PURCHASE, BOOKING_DEDUCTION, BOOKING_STATUS_UPDATE_DEDUCTION, ADMIN_CHARGE)
     * that contributed to this agency's negative balance, ordered by createdAt DESC.
     */
    private List<TransactionDto> debitTransactions;
}
