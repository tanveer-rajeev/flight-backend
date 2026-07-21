package com.aerionsoft.application.service.finance;

import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.accounthead.AccountHeadRequest;
import com.aerionsoft.application.dto.accounthead.AccountHeadResponse;
import com.aerionsoft.application.entity.AccountHead;
import com.aerionsoft.application.enums.finance.AccountHeadType;
import com.aerionsoft.application.enums.common.UsingPortal;
import com.aerionsoft.application.repository.finance.AccountHeadRepository;
import com.aerionsoft.application.util.TimestampMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountHeadService {

    @Autowired
    private AccountHeadRepository accountHeadRepository;

    @Autowired
    private TimestampMapper timestampMapper;

    @Transactional
    public AccountHeadResponse createAccountHead(AccountHeadRequest request, Long userId) {
        AccountHead accountHead = AccountHead.builder()
                .accountHeadTitle(request.getAccountHeadTitle())
                .type(request.getType())
                .parentId(request.getParentId() != null ? request.getParentId() : 0L)
                .usingPortal(request.getUsingPortal())
                .portalId(request.getPortalId())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        AccountHead savedAccountHead = accountHeadRepository.save(accountHead);
        return mapToResponse(savedAccountHead);
    }

    public AccountHeadResponse getAccountHeadById(Long id, Long userId, boolean isAdmin) {
        AccountHead accountHead;

        if (isAdmin) {
            // Admin users can access records with usingPortal = ADMIN
            accountHead = accountHeadRepository.findById(id)
                    .filter(ah -> ah.getUsingPortal() == UsingPortal.ADMIN)
                    .orElseThrow(() -> new ResourceNotFoundException("Account Head", id + " or access denied"));
        } else {
            // Regular users can only access their own records
            accountHead = accountHeadRepository.findById(id)
                    .filter(ah -> ah.getCreatedBy().equals(userId))
                    .orElseThrow(() -> new ResourceNotFoundException("Account Head", id + " or access denied"));
        }

        return mapToResponse(accountHead);
    }

    public List<AccountHeadResponse> getAllAccountHeads(Long userId, boolean isAdmin) {
        List<AccountHead> accountHeads;

        if (isAdmin) {
            // Admin users see all records with usingPortal = ADMIN
            accountHeads = accountHeadRepository.findByUsingPortal(UsingPortal.ADMIN);
        } else {
            // Regular users see only their own records
            accountHeads = accountHeadRepository.findByCreatedBy(userId);
        }

        return accountHeads.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Page<AccountHeadResponse> getAllAccountHeadsPaginated(int page, int size, String sortBy, String sortDir, Long userId, boolean isAdmin) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AccountHead> accountHeadPage;
        if (isAdmin) {
            // Admin users see all records with usingPortal = ADMIN
            accountHeadPage = accountHeadRepository.findByUsingPortal(UsingPortal.ADMIN, pageable);
        } else {
            // Regular users see only their own records
            accountHeadPage = accountHeadRepository.findByCreatedBy(userId, pageable);
        }

        return accountHeadPage.map(this::mapToResponse);
    }

    public List<AccountHeadResponse> getAccountHeadsByType(AccountHeadType type) {
        return accountHeadRepository.findByType(type).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AccountHeadResponse> getAccountHeadsByParentId(Long parentId) {
        return accountHeadRepository.findByParentId(parentId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AccountHeadResponse> getAccountHeadsByPortal(UsingPortal usingPortal) {
        return accountHeadRepository.findByUsingPortal(usingPortal).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AccountHeadResponse> getAccountHeadsByPortalAndId(UsingPortal usingPortal, Long portalId) {
        return accountHeadRepository.findByUsingPortalAndPortalId(usingPortal, portalId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountHeadResponse updateAccountHead(Long id, AccountHeadRequest request, Long userId) {
        AccountHead accountHead = accountHeadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account Head", id));

        if (request.getAccountHeadTitle() != null) {
            accountHead.setAccountHeadTitle(request.getAccountHeadTitle());
        }
        if (request.getType() != null) {
            accountHead.setType(request.getType());
        }
        if (request.getParentId() != null) {
            accountHead.setParentId(request.getParentId());
        }
        if (request.getUsingPortal() != null) {
            accountHead.setUsingPortal(request.getUsingPortal());
        }
        if (request.getPortalId() != null) {
            accountHead.setPortalId(request.getPortalId());
        }

        accountHead.setUpdatedBy(userId);

        AccountHead updatedAccountHead = accountHeadRepository.save(accountHead);
        return mapToResponse(updatedAccountHead);
    }

    @Transactional
    public void deleteAccountHead(Long id) {
        if (!accountHeadRepository.existsById(id)) {
            throw new ResourceNotFoundException("Account Head", id);
        }
        accountHeadRepository.deleteById(id);
    }

    private AccountHeadResponse mapToResponse(AccountHead accountHead) {
        return AccountHeadResponse.builder()
                .id(accountHead.getId())
                .accountHeadTitle(accountHead.getAccountHeadTitle())
                .type(accountHead.getType())
                .parentId(accountHead.getParentId())
                .createdBy(accountHead.getCreatedBy())
                .updatedBy(accountHead.getUpdatedBy())
                .createdAt(timestampMapper.toRequestUserTime(accountHead.getCreatedAt(), accountHead.getCreatedTimeOffset()))
                .updatedAt(timestampMapper.toRequestUserTime(accountHead.getUpdatedAt(), accountHead.getUpdatedTimeOffset() != null ? accountHead.getUpdatedTimeOffset() : accountHead.getCreatedTimeOffset()))
                .usingPortal(accountHead.getUsingPortal())
                .portalId(accountHead.getPortalId())
                .build();
    }
}
