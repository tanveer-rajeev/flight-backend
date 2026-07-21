package com.aerionsoft.application.entity.group;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "passenger_fares")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerFare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String fareBasis;
    private Integer quantity;
    private Integer bookedQuantity = 0; // Default value
    private String currency;
    private Double baseFare;
    private Double equivalentBaseFare;
    private Double equivalentTaxes;
    private String equivalentCurrency;
    private Double exchangeRate;
    private Double baggageKg;
}