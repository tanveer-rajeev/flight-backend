package com.aerionsoft.application.entity.group;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.entity.client.Supplier;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "group_tickets", indexes = {
        @Index(name = "idx_groupticket_gdspnr", columnList = "gdsPnr")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupTicket {
    @Id
    @Column(unique = true, nullable = false)
    private String gfCode;
    private String title;

    /** Fare/refund category, e.g. Non-Refundable. */
    private String type;

    /** Ticket category: GROUP, UMRAH, or A2A. */
    @Column(name = "ticket_type")
    private String ticketType;

    private String status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String specialInstructions;

    private String airlineCode;
    private String airlineName;
    private String vendorName;
    private LocalDate bookingStarts;
    private LocalDate bookingEnds;
    private String origin;
    private String destination;
    private String fareCurrency;

    private LocalDate departureDate;
    private String departureTime;
    private LocalDate arrivalDate;
    private String arrivalTime;

    private String flightType;

    @Column(unique = true, nullable = false)
    private String gdsPnr;

    private String airlinePnr;

    /** Supplier linked to this group ticket (optional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /** Net cost / buying price for this group ticket. */
    @Column(name = "costing")
    private Double costing;

    /**
     * Sale channel: "ONLINE" or "OFFLINE".
     * Stored as a plain VARCHAR – no enum to avoid extra migrations.
     */
    @Column(name = "sale_status", length = 20)
    private String saleStatus;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "group_ticket_id")
    private List<FlightInfo> flightInfo;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "group_ticket_id")
    private List<PassengerFare> passengerFares;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = UserDateTimeUtil.now();
        }
    }
}

