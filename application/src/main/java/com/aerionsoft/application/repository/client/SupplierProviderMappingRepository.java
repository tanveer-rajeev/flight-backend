package com.aerionsoft.application.repository.client;

import com.aerionsoft.application.entity.client.Supplier;
import com.aerionsoft.application.entity.client.SupplierProviderMapping;
import com.aerionsoft.application.enums.booking.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupplierProviderMappingRepository extends JpaRepository<SupplierProviderMapping, Long> {

    List<SupplierProviderMapping> findBySupplierId(Long supplierId);

    void deleteBySupplierId(Long supplierId);

    boolean existsByProviderAndChannelAndSupplierIdNot(Provider provider, String channel, Long supplierId);

    boolean existsByProviderAndChannelIsNullAndSupplierIdNot(Provider provider, Long supplierId);

    @Query("""
            SELECT m.supplier FROM SupplierProviderMapping m
            WHERE m.provider = :provider AND m.channel = :channel
              AND m.supplier.agencyUser IS NULL AND m.supplier.isDeleted = false
            """)
    Optional<Supplier> findAdminSupplierByProviderAndChannel(
            @Param("provider") Provider provider,
            @Param("channel") String channel
    );

    @Query("""
            SELECT m.supplier FROM SupplierProviderMapping m
            WHERE m.provider = :provider AND m.channel IS NULL
              AND m.supplier.agencyUser IS NULL AND m.supplier.isDeleted = false
            """)
    Optional<Supplier> findAdminSupplierByProviderOnly(@Param("provider") Provider provider);
}
