package com.aerionsoft.application.dto.ticketaction;

import com.aerionsoft.application.enums.booking.TicketActionType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketActionRequestCreateRequest {

    @NotNull
    private TicketActionType type;

    private String reason;
}
