package com.aerionsoft.application.dto.report;

import com.aerionsoft.application.dto.accounthead.AccountHeadShortDTO;
import com.aerionsoft.application.enums.finance.AccountHeadType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountHeadReportResponseDTO {
    private Long id;
    private AccountHeadShortDTO accountHead;
    private AccountHeadType type;
    private String title;
    private BigDecimal amount;
    private String details;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
