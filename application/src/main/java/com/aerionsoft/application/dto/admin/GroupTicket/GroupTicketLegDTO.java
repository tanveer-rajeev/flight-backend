package com.aerionsoft.application.dto.admin.GroupTicket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Groups connecting segments under one itinerary leg.
 * Optional on create/update input; always populated on responses when flightInfos exist.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupTicketLegDTO {
    private Integer leg;
    /** ONEWAY or RETURN */
    private String segmentType;
    /** First segment origin airport code. */
    private String origin;
    /** Last segment destination airport code. */
    private String destination;
    private List<FlightInfoDTO> segments;
}
