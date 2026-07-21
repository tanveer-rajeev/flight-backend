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
@Table(name = "extras")
@AllArgsConstructor
public class Extras {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id")
    private Long bookingId;

    private String seatCode;
    private String mealCode;
    private String baggageCode;
    private String flightNumber;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = UserDateTimeUtil.now();
        createdAt = now;
        updatedAt = now;
    }

}
