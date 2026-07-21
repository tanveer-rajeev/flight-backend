package com.aerionsoft.application.dto.sabre;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DailyTicketSegmentCountApiResponse {

    private boolean success;
    private String message;
    private String date;
    private String channel;
    private Integer totalPnrs;
    private Integer totalSegments;
    private List<PnrSegmentCount> pnrSegmentCounts;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PnrSegmentCount {
        private String pnr;
        private Integer segmentCount;
    }
}

