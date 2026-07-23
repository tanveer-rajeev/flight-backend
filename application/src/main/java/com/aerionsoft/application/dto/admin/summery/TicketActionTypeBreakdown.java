package com.aerionsoft.application.dto.admin.summery;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketActionTypeBreakdown {

    private Long submitted;
    private Long quoted;
    private Long userConfirmed;
    private Long adminProcessing;
    private Long totalOpen;
    private Long needsAdminAction;
}
