package com.aerionsoft.application.dto.sabre;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTicketSegmentListResponse {

    private boolean success;
    private String message;
    private LocalDate date;
    private int totalRecords;
    private int totalSegments;
    private List<SegmentRecord> records;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentRecord {
        private Long id;
        private String pnr;
        private Integer segmentCount;
        private String channel;
        private LocalDate date;
        private String createdAt;
    }
}

