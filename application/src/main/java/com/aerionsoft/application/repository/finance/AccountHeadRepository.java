package com.aerionsoft.application.repository.finance;

import com.aerionsoft.application.entity.AccountHead;
import com.aerionsoft.application.enums.finance.AccountHeadType;
import com.aerionsoft.application.enums.common.UsingPortal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface   AccountHeadRepository extends JpaRepository<AccountHead, Long> {

    List<AccountHead> findByType(AccountHeadType type);

    List<AccountHead> findByParentId(Long parentId);

    List<AccountHead> findByUsingPortal(UsingPortal usingPortal);

    List<AccountHead> findByUsingPortalAndPortalId(UsingPortal usingPortal, Long portalId);

    List<AccountHead> findByTypeAndUsingPortal(AccountHeadType type, UsingPortal usingPortal);

    // Pagination methods
    Page<AccountHead> findByUsingPortal(UsingPortal usingPortal, Pageable pageable);

    Page<AccountHead> findByCreatedBy(Long createdBy, Pageable pageable);

    // List methods for filtering
    List<AccountHead> findByCreatedBy(Long createdBy);

    List<AccountHead> findByTypeAndPortalId(AccountHeadType type, Long portalId);

    List<AccountHead> findByPortalId(Long portalId);

    List<AccountHead> findByIdAndType(Long id, AccountHeadType type);

    java.util.Optional<AccountHead> findByAccountHeadTitleAndUsingPortal(String accountHeadTitle, UsingPortal usingPortal);
}
