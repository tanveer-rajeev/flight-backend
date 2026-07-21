package com.aerionsoft.application.dto.client.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDto {
    private StatItem totalTraveller;
    private StatItem montTicket;
    private StatItem lastWeekTicket;
    private StatItem todayTicket;
    private StatItem tureApplicationLastMonth;
    private StatItem visaApplicationLastMonth;
    private StatItem transactionLastMonth;
    private StatItem depositRequestLastMonth;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatItem {
        private String title;
        private int count;
    }
}
