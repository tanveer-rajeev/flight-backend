package com.aerionsoft.application.dto.ticketaction;

import com.aerionsoft.application.enums.booking.TicketActionType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TicketActionRequestCreateRequest {

    @NotNull
    private TicketActionType type;

    private String reason;

    /**
     * Required when {@code type = REISSUE}. Preferred travel / reissue date requested by agency.
     */
    private LocalDate reissueDate;

    private String userTimeOffset;
}
