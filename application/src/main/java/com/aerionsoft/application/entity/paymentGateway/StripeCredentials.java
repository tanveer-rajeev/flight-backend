package com.aerionsoft.application.entity.paymentGateway;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "stripe_credentials")
public class StripeCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String secretKey;
    private String publishableKey;
    private String webhookSecret;
    private String clientId; // For Connect accounts
    private String accountId; // For Connect accounts
}
