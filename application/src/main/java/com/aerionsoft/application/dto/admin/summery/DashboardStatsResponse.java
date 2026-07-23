package com.aerionsoft.application.dto.admin.summery;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {

    /** @deprecated Use {@link #agencies} */
    private Long pnrStatusOnlyCount;

    /** @deprecated Use {@link #deposits} */
    private Long pendingDepositRequestCount;

    /** @deprecated Use {@link #agencies} */
    private Long newBusinessCount;

    private AdminPendingQueueStats agencies;
    private AdminPendingQueueStats deposits;
    private BookingStatusPendingStats bookings;
    private TicketActionPendingStats ticketActions;
    private AdminPendingSummary summary;

}

