package com.aerionsoft.application.dto.audit;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ActivityFeedAgencyInfo {

    private Long businessId;
    private Long agencyUserId;
    private String agencyName;
    private String agencyEmail;
    private String agencyPhone;
    private String agencyCurrency;
    /** Sub-user or booking owner when different from agency account. */
    private Long ownerUserId;
    private String ownerUserName;
    private String ownerUserEmail;
}
