package com.aerionsoft.application.repository.business;

import com.aerionsoft.application.entity.BusinessProvider;
import com.aerionsoft.application.enums.booking.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessProviderRepository extends JpaRepository<BusinessProvider, Long> {

    List<BusinessProvider> findByBusinessId(Long businessId);

    Optional<BusinessProvider> findByBusinessIdAndProvider(Long businessId, Provider provider);

    boolean existsByBusinessIdAndProvider(Long businessId, Provider provider);

    void deleteByBusinessIdAndProvider(Long businessId, Provider provider);

    void deleteByBusinessId(Long businessId);

    @Query("SELECT bp.provider FROM BusinessProvider bp WHERE bp.business.id = :businessId")
    List<Provider> findProvidersByBusinessId(@Param("businessId") Long businessId);
}

