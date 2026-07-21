package com.aerionsoft.application.repository.client;

import com.aerionsoft.application.entity.client.Invoice;
import com.aerionsoft.application.entity.client.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    Page<Invoice> findByAgencyUserIsNull(Pageable pageable);

    Optional<Invoice> findByIdAndAgencyUserIsNull(Long id);

    Page<Invoice> findByAgencyUser(User agencyUser, Pageable pageable);

    Optional<Invoice> findByIdAndAgencyUser(Long id, User agencyUser);

    @Query("SELECT COALESCE(SUM(i.invoiceAmount), 0) FROM Invoice i WHERE (:agencyId IS NULL OR i.agencyUser.id = :agencyId)")
    BigDecimal getTotalInvoiceAmount(@Param("agencyId") Long agencyId);

    @Query("SELECT COALESCE(SUM(i.invoiceRevenue), 0) FROM Invoice i WHERE (:agencyId IS NULL OR i.agencyUser.id = :agencyId)")
    BigDecimal getTotalInvoiceRevenue(@Param("agencyId") Long agencyId);

    List<Invoice> findByInvoiceDetailsContaining(String fragment);

}
