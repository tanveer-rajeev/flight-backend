package com.aerionsoft.application.repository.client;

import com.aerionsoft.application.entity.client.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceLedgerRepository extends JpaRepository<Ledger, Long> {
    List<Ledger> findAllByAgencyIdIsNull();

    List<Ledger> findAllByAgencyId(Long agencyId);

    Optional<Ledger> findByIdAndAgencyId(Long id, Long agencyId);

    Optional<Ledger> findByIdAndAgencyIdIsNull(Long id);

    Optional<Ledger> findByTitleAndAgencyIdIsNull(String title);
}
