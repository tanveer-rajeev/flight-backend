package com.aerionsoft.application.repository.client;

import com.aerionsoft.application.entity.client.DynamicService;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.enums.client.InvoiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceDynamicServiceRepository extends JpaRepository<DynamicService,Long> {
    Optional<DynamicService> findByServiceKeyAndServiceType(String serviceKey, InvoiceType serviceType);

    Optional<DynamicService> findByServiceKeyAndServiceTypeAndAgencyUser(String serviceKey, InvoiceType serviceType, User agencyUser);

    List<DynamicService> findByAgencyUserIsNull();

    List<DynamicService> findByAgencyUser(User agencyUser);

    Optional<DynamicService> findByIdAndAgencyUser(Long id, User agencyUser);

    List<DynamicService> findByServiceType(InvoiceType serviceType);

    List<DynamicService> findByServiceTypeAndAgencyUser(InvoiceType serviceType , User agencyUser);
}
