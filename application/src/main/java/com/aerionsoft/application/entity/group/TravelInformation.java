package com.aerionsoft.application.entity.group;


import com.aerionsoft.application.entity.Booking.Booking;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "travel_information")
public class TravelInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "booking_id")
    private Long bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Booking booking;

    @Column(name = "airline_name")
    private String airlineName;

    @Column(name = "flight_number")
    private String flightNumber;

    private String origin;
    private String destination;

    @Column(name = "departure_airport")
    private String departureAirport;

    @Column(name = "arrival_airport")
    private String arrivalAirport;

    @Column(name = "departure_date")
    private String departureDate;

    @Column(name = "departure_time")
    private String departureTime;

    @Column(name = "arrival_date")
    private String arrivalDate;

    @Column(name = "arrival_time")
    private String arrivalTime;

    @Column(name = "fare_basis")
    private String fareBasis;

    private Integer quantity;
    private String currency;

    @Column(name = "base_fare")
    private Double baseFare;

    @Column(name = "equivalent_base_fare")
    private Double equivalentBaseFare;

    @Column(name = "baggage_kg")
    private Double baggageKg;

    private Double tax;
    private String duration;

    @Column(name = "ticket_number")
    private String ticketNumber;

    private String instructions;

    @Column(name = "flight_type")
    private String flightType;

    @Column(name = "airline_code")
    private String airlineCode;

    @Column(name = "oneway_segment_stop_count")
    private int onewaySegmentStopCount = 0;
    @Column(name = "return_segment_stop_count")
    private int returnSegmentStopCount = 0;
}