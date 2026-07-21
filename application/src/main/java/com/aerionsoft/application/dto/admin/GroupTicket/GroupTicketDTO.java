package com.aerionsoft.application.dto.admin.GroupTicket;

import com.aerionsoft.application.enums.group.GroupTicketType;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupTicketDTO {
    @NotBlank(message = "GF code must not be blank")
    private String gfCode;
    @NotBlank(message = "Title must not be blank")
    private String title;
    @NotBlank(message = "Type must not be blank")
    private String type;
    /** Ticket category: GROUP, UMRAH, or A2A. */
    @NotNull(message = "Ticket type must not be null")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private GroupTicketType ticketType;
    @NotBlank(message = "Status must not be blank")
    private String status;
    private String description;
    private String specialInstructions;
    @NotBlank(message = "Airline code must not be blank")
    private String airlineCode;
    @NotBlank(message = "Airline name must not be blank")
    private String airlineName;
    @NotBlank(message = "Vendor name must not be blank")
    private String vendorName;
    @NotNull(message = "Booking start date must not be null")
    private LocalDate bookingStarts;
    @NotNull(message = "Booking end date must not be null")
    private LocalDate bookingEnds;
    @NotBlank(message = "Origin must not be blank")
    private String origin;
    @NotBlank(message = "Destination must not be blank")
    private String destination;
    @NotBlank(message = "Fare currency must not be blank")
    private String fareCurrency;
    private String gdsPnr;
    private String airlinePnr;

    @NotNull(message = "Departure date must not be null")
    private LocalDate departureDate;

    @NotBlank(message = "Departure time must not be blank")
    private String departureTime;

    @NotNull(message = "Arrival date must not be null")
    private LocalDate arrivalDate;

    @NotBlank(message = "Arrival time must not be blank")
    private String arrivalTime;

    @NotBlank(message = "Flight type must not be blank")
    private String flightType;

    // ── Supplier ──────────────────────────────────────────────────────────────
    /** ID of the linked supplier (optional on input, resolved to name on output). */
    private Long supplierId;

    /** Supplier name (internal/code) – populated on responses, ignored on create/update input. */
    private String supplierName;

    /** Supplier display title – populated on responses, ignored on create/update input. */
    private String supplierTitle;

    // ── Costing & sale channel ────────────────────────────────────────────────
    /** Net cost / buying price for this group ticket. */
    private Double costing;

    /**
     * Sale channel: "ONLINE" or "OFFLINE".
     */
    private String saleStatus;

    // ── Sub-lists ─────────────────────────────────────────────────────────────
    /** Flat segment list (stored). Each item includes leg + segmentType. */
    private List<FlightInfoDTO> flightInfos;
    /**
     * Optional grouped input for the admin UI. When provided on create/update, segments are
     * flattened into {@link #flightInfos}. Always populated on responses when flightInfos exist.
     */
    private List<GroupTicketLegDTO> legs;
    private List<PassengerFareDTO> passengerFares;
}
