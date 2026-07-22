package com.aerionsoft.application.dto.admin.summery;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketActionPendingStats {

    /** All open ticket actions combined */
    private TicketActionTypeBreakdown totals;

    private TicketActionTypeBreakdown cancel;

    @JsonProperty("void")
    private TicketActionTypeBreakdown voidType;

    private TicketActionTypeBreakdown refund;

    private TicketActionTypeBreakdown reissue;
}
