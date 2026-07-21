package com.aerionsoft.application.dto.expense;

import com.aerionsoft.application.enums.common.UsingPortal;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseDetailResponse {

    private Long id;
    private Long expenseId;
    private Long accountHeadId;
    private String accountHeadTitle;
    private String itemTitle;
    private String itemDescription;
    private BigDecimal itemAmount;
    private String itemAttachment;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UsingPortal usingPortal;
    private Long portalId;
}
