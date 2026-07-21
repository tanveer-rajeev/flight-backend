package com.aerionsoft.application.dto.sabre;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketingDeadlineResponse {
    private Boolean success;
    private String message;
    private Integer status;
    private TicketingDeadlineData data;
}

