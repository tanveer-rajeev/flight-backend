package com.aerionsoft.application.dto.admin.summery;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingStatusPendingStats {

    private Long process;

    private Long pnr;

    @JsonProperty("onHold")
    private Long onHold;

    private Long book;

    private Long confirmed;

    @JsonProperty("validationProcess")
    private Long validationProcess;

    @JsonProperty("validationPriceChanged")
    private Long validationPriceChanged;

    private Long reprice;

    /** Sum of all in-progress statuses above */
    private Long totalOpen;

    /** Bookings that typically need admin/system follow-up */
    private Long needsAdminAction;

    /** @deprecated Use {@link #pnr} */
    private Long pendingApproval;
}

