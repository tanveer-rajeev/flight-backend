package com.aerionsoft.application.dto.admin.summery;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {

    private Long pnrStatusOnlyCount;
    private Long pendingDepositRequestCount;
    private Long newBusinessCount;

}

