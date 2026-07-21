package com.aerionsoft.application.entity.Booking;

import com.aerionsoft.application.util.UserDateTimeUtil;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@Entity
@Table(name = "booking_package_baggage")
@AllArgsConstructor
public class BookingPackageBaggage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id")
    private Long bookingId;

    private String pax;
    private Double weight;
    private String unit;
    private Integer flightNumber;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = UserDateTimeUtil.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = UserDateTimeUtil.now();
    }
}
