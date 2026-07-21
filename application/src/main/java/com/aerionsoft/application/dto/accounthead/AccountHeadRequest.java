package com.aerionsoft.application.dto.accounthead;

import com.aerionsoft.application.enums.finance.AccountHeadType;
import com.aerionsoft.application.enums.common.UsingPortal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountHeadRequest {

    private String accountHeadTitle;
    private AccountHeadType type;
    private Long parentId;
    private UsingPortal usingPortal;
    private Long portalId;
}

