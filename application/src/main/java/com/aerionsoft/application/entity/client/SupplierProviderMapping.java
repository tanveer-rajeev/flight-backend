package com.aerionsoft.application.entity.client;

import com.aerionsoft.application.entity.converter.ProviderAttributeConverter;
import com.aerionsoft.application.enums.booking.Provider;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "supplier_provider_mappings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierProviderMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Convert(converter = ProviderAttributeConverter.class)
    @Column(nullable = false, length = 50)
    private Provider provider;

    @Column(length = 100)
    private String channel;
}
