package com.aerionsoft.application.dto.booking.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PassengerTicketDTO {
    private String name;
    private String ticketNumber;
}
