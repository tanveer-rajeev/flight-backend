package com.aerionsoft.application.dto.sabre;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketingDeadlineData {
    private String pnr;
    private String ticketingDeadline;
    private String message;
    private Boolean success;

    @JsonProperty("secondsUntilDeadline")
    private Long secondsUntilDeadline;

    @JsonProperty("bookedTimeOffset")
    private String bookedTimeOffset;
}

