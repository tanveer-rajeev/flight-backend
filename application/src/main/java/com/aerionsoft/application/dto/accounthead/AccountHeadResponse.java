package com.aerionsoft.application.dto.accounthead;

import com.aerionsoft.application.enums.finance.AccountHeadType;
import com.aerionsoft.application.enums.common.UsingPortal;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountHeadResponse {

    private Long id;
    private String accountHeadTitle;
    private AccountHeadType type;
    private Long parentId;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UsingPortal usingPortal;
    private Long portalId;
}

