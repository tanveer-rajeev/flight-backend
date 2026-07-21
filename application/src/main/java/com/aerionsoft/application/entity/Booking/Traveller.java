package com.aerionsoft.application.entity.Booking;

import com.aerionsoft.application.enums.user.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;

@Entity
@Table(name = "travellers")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Traveller {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // MR / MS

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String mobile;

    @Column(nullable = false)
    private String mobileCountryCode;

    @Column(nullable = true)
    private String email; // optional, can be null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false)
    private LocalDate dob;

    private String passportNo;

    @Column(nullable = true)
    private LocalDate passportIssueDate;

    private LocalDate passportExpiryDate;

    @Column(nullable = false)
    private String countryName;

    @Column(nullable = false)
    private String countryCode;

    @Column(nullable = false)
    private String cityName;

    @Column(nullable = false)
    private String cityCode;

    @Column(nullable = false)
    private String mealCode;

    // Keep nationality for backward compatibility
    private String nationality;

    private String addressLine1;

    private String addressLine2;

    private Long createdBy;

    @CreationTimestamp
    private LocalDate createdDate;

    @UpdateTimestamp
    private LocalDate updatedDate;


    @Column(name = "visaOrNidImageURL")
    private String visaOrNidImageURL;

    @Column(name = "passportImageUrl")
    private String passportImageUrl;
}