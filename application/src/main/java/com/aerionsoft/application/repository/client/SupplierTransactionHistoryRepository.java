package com.aerionsoft.application.repository.client;

import com.aerionsoft.application.entity.client.SupplierTransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SupplierTransactionHistoryRepository extends JpaRepository<SupplierTransactionHistory,Long>, JpaSpecificationExecutor<SupplierTransactionHistory> {
    // admin → agency_id IS NULL
    Page<SupplierTransactionHistory> findAllByAgencyIdIsNull(Pageable pageable);

    // agency → agency_id = ?
    Page<SupplierTransactionHistory> findAllByAgencyId(Long agencyId, Pageable pageable);

    List<SupplierTransactionHistory> findByInvoiceIdAndDescriptionNotContaining(Long invoiceId, String marker);

    List<SupplierTransactionHistory> findByDescriptionContainingAndDescriptionNotContaining(
            String contains, String notContains);

    List<SupplierTransactionHistory> findBySupplierId(Long supplierId);
}
