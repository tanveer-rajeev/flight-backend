package com.aerionsoft.application.repository.client;

import com.aerionsoft.application.entity.client.Supplier;
import com.aerionsoft.application.entity.client.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findAllByAgencyUserIsNull();
    List<Supplier> findAllByAgencyUser(User agencyUser);
    Optional<Supplier> findByIdAndAgencyUserIsNull(Long id);
    Optional<Supplier> findByIdAndAgencyUser(Long id, User agencyUser);
    Optional<Supplier> findByNameAndAgencyUserIsNull(String name);

    List<Supplier> findAllByAgencyUserIsNullAndBranch_Id(Long branchId);
    List<Supplier> findAllByAgencyUserAndBranch_Id(User agencyUser, Long branchId);

    long countByBranch_IdAndIsDeletedFalse(Long branchId);

    List<Supplier> findByIdInAndAgencyUserIsNull(Collection<Long> ids);

    List<Supplier> findByIdInAndAgencyUser(Collection<Long> ids, User agencyUser);
}
