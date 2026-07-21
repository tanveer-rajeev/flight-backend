package com.aerionsoft.application.repository.client;

import com.aerionsoft.application.entity.client.Branch;
import com.aerionsoft.application.entity.client.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    List<Branch> findAllByAgencyUserIsNullAndIsDeletedFalseOrderByNameAsc();

    List<Branch> findAllByAgencyUserAndIsDeletedFalseOrderByNameAsc(User agencyUser);

    List<Branch> findAllByAgencyUserIsNullAndIsDeletedFalseAndIsActiveTrueOrderByNameAsc();

    List<Branch> findAllByAgencyUserAndIsDeletedFalseAndIsActiveTrueOrderByNameAsc(User agencyUser);

    Optional<Branch> findByIdAndAgencyUserIsNullAndIsDeletedFalse(Long id);

    Optional<Branch> findByIdAndAgencyUserAndIsDeletedFalse(Long id, User agencyUser);
}
