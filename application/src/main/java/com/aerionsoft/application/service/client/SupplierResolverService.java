package com.aerionsoft.application.service.client;

import com.aerionsoft.application.entity.client.Supplier;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.repository.client.SupplierProviderMappingRepository;
import com.aerionsoft.application.repository.client.SupplierRepository;
import com.aerionsoft.application.service.common.PlatformProviderService;
import org.springframework.stereotype.Service;

@Service
public class SupplierResolverService {

    private final SupplierRepository supplierRepository;
    private final SupplierProviderMappingRepository mappingRepository;
    private final PlatformProviderService platformProviderService;

    public SupplierResolverService(
            SupplierRepository supplierRepository,
            SupplierProviderMappingRepository mappingRepository,
            PlatformProviderService platformProviderService
    ) {
        this.supplierRepository = supplierRepository;
        this.mappingRepository = mappingRepository;
        this.platformProviderService = platformProviderService;
    }

    /**
     * Resolves an admin supplier for live/online bookings using configured provider mappings,
     * falling back to the default supplier when no mapping matches.
     */
    public Supplier resolveForLiveBooking(Provider provider, String channel) {
        if (provider == null) {
            return resolveDefaultAdminSupplier();
        }

        String resolvedChannel = resolveChannel(channel, provider);
        if (resolvedChannel != null) {
            var mapped = mappingRepository.findAdminSupplierByProviderAndChannel(provider, resolvedChannel);
            if (mapped.isPresent()) {
                return mapped.get();
            }
        }

        var providerOnly = mappingRepository.findAdminSupplierByProviderOnly(provider);
        if (providerOnly.isPresent()) {
            return providerOnly.get();
        }

        return resolveDefaultAdminSupplier();
    }

    public Supplier resolveDefaultAdminSupplier() {
        return mappingRepository.findAdminSupplierByProviderOnly(Provider.OTHERS)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Default admin supplier is not configured. Assign Provider.OTHERS to a supplier."));
    }

    public Supplier resolveForManualBooking(Long supplierId) {
        if (supplierId != null) {
            return supplierRepository.findByIdAndAgencyUserIsNull(supplierId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));
        }
        return resolveDefaultAdminSupplier();
    }

    private String resolveChannel(String channel, Provider provider) {
        if (channel != null && !channel.isBlank()) {
            return channel.trim();
        }
        return platformProviderService.getChannelName(provider.getValue());
    }
}
