package com.aerionsoft.application.service.business;

import com.aerionsoft.application.enums.booking.Provider;

import java.util.List;

public interface BusinessProviderService {

    /** Returns all providers assigned to a business. */
    List<Provider> getProviders(Long businessId);

    /** Assign a provider to a business. Idempotent – does nothing if already assigned. */
    void addProvider(Long businessId, Provider provider);

    /** Remove a provider from a business. */
    void removeProvider(Long businessId, Provider provider);

    /** Replace the full provider list for a business. */
    void setProviders(Long businessId, List<Provider> providers);
}

