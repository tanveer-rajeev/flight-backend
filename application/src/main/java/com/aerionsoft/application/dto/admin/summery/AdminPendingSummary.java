package com.aerionsoft.application.dto.admin.summery;

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
public class AdminPendingSummary {

    /** Sum of all actionable pending items across agencies, deposits, PNR bookings, ticket actions */
    private Long totalPendingItems;
}

