package com.aerionsoft.application.dto.sabre;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTicketSegmentReloadResponse {

    private boolean success;
    private String message;
    private String date;
    private String channel;
    private int newRecordsSaved;
    private int skippedDuplicates;
    private int totalFromApi;
    private int totalStoredForDay;

    /** All records currently stored for this channel+date (including today's existing + newly saved). */
    private List<DailyTicketSegmentListResponse.SegmentRecord> records;
}

