package com.aerionsoft.application.repository.wallet;

import com.aerionsoft.application.entity.CreditRequest;
import com.aerionsoft.application.enums.wallet.CreditRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditRequestRepository extends JpaRepository<CreditRequest, Long> {

    Page<CreditRequest> findByBusinessId(Long businessId, Pageable pageable);

    Page<CreditRequest> findByBusinessIdAndStatus(Long businessId, CreditRequestStatus status, Pageable pageable);

    Page<CreditRequest> findByStatus(CreditRequestStatus status, Pageable pageable);

    Page<CreditRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<CreditRequest> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    void deleteByBusinessId(Long businessId);

    long countByBusinessIdAndStatus(Long businessId, CreditRequestStatus status);
}

