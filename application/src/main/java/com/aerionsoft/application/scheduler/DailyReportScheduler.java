package com.aerionsoft.application.scheduler;

import com.aerionsoft.application.dto.DailyReportResponseDTO;
import com.aerionsoft.application.service.report.DailyReportService;
import com.aerionsoft.application.service.notification.NotificationHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyReportScheduler {
    private final DailyReportService dailyReportService;
    private final NotificationHelper notificationHelper;

    public DailyReportScheduler(DailyReportService dailyReportService, NotificationHelper notificationHelper) {
        this.dailyReportService = dailyReportService;
        this.notificationHelper = notificationHelper;
    }

    @Scheduled(cron = "0 0 12 * * ?", zone = "Asia/Dhaka")
    public void run() {
        DailyReportResponseDTO report = dailyReportService.generateDailyReport();
        notificationHelper.sendDailyReport(report);
    }
}
