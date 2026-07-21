package com.aerionsoft.application.repository.group;

import com.aerionsoft.application.entity.tour.Pricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricingRepository extends JpaRepository<Pricing, Long> {

    List<Pricing> findByPackageId(Long packageId);

    List<Pricing> findByPackageIdAndCategory(Long packageId, Pricing.PricingCategory category);

    void deleteByPackageId(Long packageId);
}
