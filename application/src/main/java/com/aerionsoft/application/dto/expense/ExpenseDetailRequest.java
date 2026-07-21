package com.aerionsoft.application.dto.expense;

import com.aerionsoft.application.enums.common.UsingPortal;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseDetailRequest {

    private Long accountHeadId;
    private String itemTitle;
    private String itemDescription;
    private BigDecimal itemAmount;
    private String itemAttachment;
    private UsingPortal usingPortal;
    private Long portalId;
}

