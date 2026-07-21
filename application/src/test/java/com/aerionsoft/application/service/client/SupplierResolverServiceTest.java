package com.aerionsoft.application.service.client;

import com.aerionsoft.application.entity.client.Supplier;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.repository.client.SupplierProviderMappingRepository;
import com.aerionsoft.application.repository.client.SupplierRepository;
import com.aerionsoft.application.service.common.PlatformProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierResolverServiceTest {

    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private SupplierProviderMappingRepository mappingRepository;
    @Mock
    private PlatformProviderService platformProviderService;

    private SupplierResolverService supplierResolverService;

    @BeforeEach
    void setUp() {
        supplierResolverService = new SupplierResolverService(
                supplierRepository,
                mappingRepository,
                platformProviderService
        );
    }

    @Test
    void resolveForLiveBooking_usesExactProviderChannelMapping() {
        Supplier mapped = supplier(10L);
        when(platformProviderService.getChannelName("SABRE")).thenReturn("s-bd");
        when(mappingRepository.findAdminSupplierByProviderAndChannel(Provider.SABRE, "s-bd"))
                .thenReturn(Optional.of(mapped));

        Supplier result = supplierResolverService.resolveForLiveBooking(Provider.SABRE, null);

        assertEquals(10L, result.getId());
    }

    @Test
    void resolveForLiveBooking_fallsBackToOthersSupplier() {
        Supplier othersSupplier = supplier(99L);
        when(platformProviderService.getChannelName("VERTEIL")).thenReturn("v-bd");
        when(mappingRepository.findAdminSupplierByProviderAndChannel(Provider.VERTEIL, "v-bd"))
                .thenReturn(Optional.empty());
        when(mappingRepository.findAdminSupplierByProviderOnly(Provider.VERTEIL))
                .thenReturn(Optional.empty());
        when(mappingRepository.findAdminSupplierByProviderOnly(Provider.OTHERS))
                .thenReturn(Optional.of(othersSupplier));

        Supplier result = supplierResolverService.resolveForLiveBooking(Provider.VERTEIL, null);

        assertEquals(99L, result.getId());
    }

    @Test
    void resolveDefaultAdminSupplier_usesOthersMapping() {
        Supplier othersSupplier = supplier(99L);
        when(mappingRepository.findAdminSupplierByProviderOnly(Provider.OTHERS))
                .thenReturn(Optional.of(othersSupplier));

        Supplier result = supplierResolverService.resolveDefaultAdminSupplier();

        assertEquals(99L, result.getId());
    }

    @Test
    void resolveDefaultAdminSupplier_throwsWhenOthersNotConfigured() {
        when(mappingRepository.findAdminSupplierByProviderOnly(Provider.OTHERS))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, supplierResolverService::resolveDefaultAdminSupplier);
    }

    private Supplier supplier(Long id) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        return supplier;
    }
}
