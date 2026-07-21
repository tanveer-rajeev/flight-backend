package com.aerionsoft.application.entity.client;

import com.aerionsoft.application.enums.client.InvoiceType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dynamic_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DynamicService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private User agencyUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private InvoiceType serviceType;

    @Column(name = "service_key", nullable = false)
    private String serviceKey;

    @Column(nullable = false)
    private Integer step;
}

