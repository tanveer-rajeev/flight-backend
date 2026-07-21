package com.aerionsoft.application.service.client;

import com.aerionsoft.application.service.common.PlatformProviderService;
import com.aerionsoft.application.dto.platform.PlatformProviderChannel;
import com.aerionsoft.application.entity.client.Supplier;
import com.aerionsoft.application.enums.booking.Provider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves admin suppliers for bookings using configured provider mappings.
 * Platform provider/channel options are cached at startup via {@link PlatformProviderService}.
 */
@Service
public class PlatformSupplierSeedService {

    private final SupplierResolverService supplierResolverService;

    public PlatformSupplierSeedService(SupplierResolverService supplierResolverService) {
        this.supplierResolverService = supplierResolverService;
    }

    @Transactional(readOnly = true)
    public Supplier resolveAdminSupplierByChannel(String channel, Provider provider) {
        return supplierResolverService.resolveForLiveBooking(provider, channel);
    }

    /**
     * @deprecated Platform suppliers are configured via supplier provider mappings, not auto-seeded.
     */
    @Deprecated
    @Transactional
    public boolean ensureSupplierExists(PlatformProviderChannel entry) {
        return false;
    }
}
