package com.aerionsoft.application.dto.admin.summery;

import com.aerionsoft.application.dto.common.UserShortDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LastTenDeposits {

    private String createdAt;
    private String status;
    private String amount;
    private Long depositId;
    private String type;
    private UserShortDto user;
    private UserShortDto agencyUser;
}
