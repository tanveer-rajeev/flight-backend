package com.aerionsoft.application.service.business;

import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.entity.BusinessProvider;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.repository.business.BusinessProviderRepository;
import com.aerionsoft.application.repository.business.BusinessRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessProviderServiceImpl implements BusinessProviderService {

    private final BusinessProviderRepository businessProviderRepository;
    private final BusinessRepository businessRepository;

    @Override
    public List<Provider> getProviders(Long businessId) {
        return businessProviderRepository.findProvidersByBusinessId(businessId);
    }

    @Override
    @Transactional
    public void addProvider(Long businessId, Provider provider) {
        if (businessProviderRepository.existsByBusinessIdAndProvider(businessId, provider)) {
            return; // already assigned
        }
        BusinessEntity business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", businessId));
        businessProviderRepository.save(
                BusinessProvider.builder()
                        .business(business)
                        .provider(provider)
                        .build()
        );
    }

    @Override
    @Transactional
    public void removeProvider(Long businessId, Provider provider) {
        businessProviderRepository.deleteByBusinessIdAndProvider(businessId, provider);
    }

    @Override
    @Transactional
    public void setProviders(Long businessId, List<Provider> providers) {
        // Remove all existing
        List<BusinessProvider> existing = businessProviderRepository.findByBusinessId(businessId);
        businessProviderRepository.deleteAll(existing);

        if (providers == null || providers.isEmpty()) return;

        BusinessEntity business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", businessId));

        List<BusinessProvider> toSave = providers.stream()
                .distinct()
                .map(p -> BusinessProvider.builder().business(business).provider(p).build())
                .toList();
        businessProviderRepository.saveAll(toSave);
    }
}

