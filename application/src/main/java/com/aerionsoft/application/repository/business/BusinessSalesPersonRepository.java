package com.aerionsoft.application.repository.business;

import com.aerionsoft.application.entity.BusinessSalesPerson;
import com.aerionsoft.application.entity.admin.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessSalesPersonRepository extends JpaRepository<BusinessSalesPerson, Long> {

    List<BusinessSalesPerson> findByBusinessId(Long businessId);

    Optional<BusinessSalesPerson> findByBusinessIdAndSalesPersonId(Long businessId, Long salesPersonId);

    boolean existsByBusinessIdAndSalesPersonId(Long businessId, Long salesPersonId);

    void deleteByBusinessIdAndSalesPersonId(Long businessId, Long salesPersonId);

    void deleteByBusinessId(Long businessId);

    @Query("SELECT bsp.salesPerson FROM BusinessSalesPerson bsp WHERE bsp.business.id = :businessId")
    List<AdminUser> findSalesPersonsByBusinessId(@Param("businessId") Long businessId);
}
