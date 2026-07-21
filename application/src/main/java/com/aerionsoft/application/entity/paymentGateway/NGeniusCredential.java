package com.aerionsoft.application.entity.paymentGateway;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "n_genius_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NGeniusCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "outlet_reference", nullable = false)
    private String outletReference;

    @Column(name = "api_key", nullable = false)
    private String apiKey;

    @Column(name = "redirect_url", nullable = false)
    private String redirectUrl;

    @Column(name = "cancel_url", nullable = false)
    private String cancelUrl;

    private boolean isActive = true;
}
