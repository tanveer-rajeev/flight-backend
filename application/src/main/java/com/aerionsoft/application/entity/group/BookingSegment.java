package com.aerionsoft.application.entity.group;

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
@Table(name = "booking_segment")
public class BookingSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "travel_information_id")
    private Long travelInformationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_information_id", referencedColumnName = "id", insertable = false, updatable = false)
    private TravelInformation travelInformation;

    @Column(name = "baggage_piece_count")
    private String baggagePieceCount;

    // Duration in minutes
    @Column(name = "duration")
    private Integer duration;

    // Cabin and baggage
    @Column(name = "cabin_class")
    private String cabinClass;

    @Column(name = "no_of_seat_available")
    private Integer noOfSeatAvailable;

    @Column(name = "cabin_baggage")
    private String cabinBaggage;

    @Column(name = "baggage")
    private String baggage;

    @Column(name = "booking_code")
    private String bookingCode;

    // Segment order and type
    @Column(name = "segment_order")
    private Integer segmentOrder;

    @Column(name = "segment_type")
    private String segmentType; // ONEWAY, RETURN
}

