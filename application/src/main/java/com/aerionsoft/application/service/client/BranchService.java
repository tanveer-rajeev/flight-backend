package com.aerionsoft.application.service.client;

import com.aerionsoft.application.dto.client.branch.BranchDto;
import com.aerionsoft.application.dto.client.branch.BranchResponseDto;
import com.aerionsoft.application.entity.client.Branch;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.repository.client.BranchRepository;
import com.aerionsoft.application.repository.client.SupplierRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.util.TimestampMapper;
import com.aerionsoft.application.util.UserDateTimeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BranchService {

    private final BranchRepository branchRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final TimestampMapper timestampMapper;

    public BranchService(
            BranchRepository branchRepository,
            SupplierRepository supplierRepository,
            UserRepository userRepository,
            TimestampMapper timestampMapper
    ) {
        this.branchRepository = branchRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.timestampMapper = timestampMapper;
    }

    @Transactional(readOnly = true)
    public List<BranchResponseDto> getAllBranches(String provider, Long authUserId, Boolean activeOnly) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);

        List<Branch> branches;
        if (isAdmin) {
            branches = Boolean.TRUE.equals(activeOnly)
                    ? branchRepository.findAllByAgencyUserIsNullAndIsDeletedFalseAndIsActiveTrueOrderByNameAsc()
                    : branchRepository.findAllByAgencyUserIsNullAndIsDeletedFalseOrderByNameAsc();
        } else {
            User agencyUser = resolveAgencyUser(authUserId);
            branches = Boolean.TRUE.equals(activeOnly)
                    ? branchRepository.findAllByAgencyUserAndIsDeletedFalseAndIsActiveTrueOrderByNameAsc(agencyUser)
                    : branchRepository.findAllByAgencyUserAndIsDeletedFalseOrderByNameAsc(agencyUser);
        }

        return branches.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BranchResponseDto getBranchById(String provider, Long authUserId, Long id) {
        Branch branch = findScopedBranch(provider, authUserId, id);
        return toDto(branch);
    }

    @Transactional
    public BranchResponseDto createBranch(String provider, Long authUserId, BranchDto branchDto) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);
        Branch branch = Branch.builder()
                .name(branchDto.getName().trim())
                .description(branchDto.getDescription())
                .address(branchDto.getAddress())
                .phoneNumber(branchDto.getPhoneNumber())
                .currency(normalizeCurrency(branchDto.getCurrency()))
                .isActive(branchDto.getIsActive() != null ? branchDto.getIsActive() : true)
                .isDeleted(false)
                .createdBy(authUserId)
                .createAt(UserDateTimeUtil.now())
                .build();

        if (!isAdmin) {
            branch.setAgencyUser(resolveAgencyUser(authUserId));
        }

        return toDto(branchRepository.save(branch));
    }

    @Transactional
    public BranchResponseDto updateBranch(String provider, Long authUserId, Long id, BranchDto branchDto) {
        Branch branch = findScopedBranch(provider, authUserId, id);

        branch.setName(branchDto.getName().trim());
        branch.setDescription(branchDto.getDescription());
        branch.setAddress(branchDto.getAddress());
        branch.setPhoneNumber(branchDto.getPhoneNumber());
        if (branchDto.getCurrency() != null && !branchDto.getCurrency().isBlank()) {
            branch.setCurrency(normalizeCurrency(branchDto.getCurrency()));
        }
        if (branchDto.getIsActive() != null) {
            branch.setIsActive(branchDto.getIsActive());
        }
        branch.setUpdatedBy(authUserId);
        branch.setUpdateAt(UserDateTimeUtil.now());

        return toDto(branchRepository.save(branch));
    }

    @Transactional
    public void deleteBranch(String provider, Long authUserId, Long id) {
        Branch branch = findScopedBranch(provider, authUserId, id);

        if (supplierRepository.countByBranch_IdAndIsDeletedFalse(id) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Cannot delete branch that is assigned to suppliers");
        }

        branch.setIsDeleted(true);
        branch.setUpdatedBy(authUserId);
        branch.setUpdateAt(UserDateTimeUtil.now());
        branchRepository.save(branch);
    }

    /**
     * Ensures the branch exists in the caller's scope. Does not require the branch to be active.
     */
    public void ensureBranchAccessible(String provider, Long authUserId, Long branchId) {
        findScopedBranch(provider, authUserId, branchId);
    }

    /**
     * Resolves a branch for supplier assignment. Branch must belong to the same scope
     * (admin-global or agency) as the supplier being created/updated.
     */
    public Branch resolveBranchForSupplier(String provider, Long authUserId, Long branchId) {
        if (branchId == null) {
            return null;
        }
        Branch branch = findScopedBranch(provider, authUserId, branchId);
        if (!Boolean.TRUE.equals(branch.getIsActive())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Branch is inactive");
        }
        return branch;
    }

    private Branch findScopedBranch(String provider, Long authUserId, Long id) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);

        if (isAdmin) {
            return branchRepository.findByIdAndAgencyUserIsNullAndIsDeletedFalse(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
        }

        User agencyUser = resolveAgencyUser(authUserId);
        return branchRepository.findByIdAndAgencyUserAndIsDeletedFalse(id, agencyUser)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
    }

    private User resolveAgencyUser(Long authUserId) {
        User user = userRepository.findById(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        return user.getParentUser() != null ? user.getParentUser() : user;
    }

    private BranchResponseDto toDto(Branch branch) {
        BranchResponseDto dto = new BranchResponseDto();
        dto.setId(branch.getId());
        dto.setAgencyId(branch.getAgencyUser() != null ? branch.getAgencyUser().getId() : null);
        dto.setName(branch.getName());
        dto.setDescription(branch.getDescription());
        dto.setAddress(branch.getAddress());
        dto.setPhoneNumber(branch.getPhoneNumber());
        dto.setCurrency(branch.getCurrency());
        dto.setIsActive(branch.getIsActive());
        dto.setIsDeleted(branch.getIsDeleted());
        dto.setCreatedBy(branch.getCreatedBy());
        dto.setUpdatedBy(branch.getUpdatedBy());
        dto.setCreatedAt(timestampMapper.toRequestUserTime(branch.getCreateAt(), branch.getCreatedTimeOffset()));
        dto.setUpdatedAt(timestampMapper.toRequestUserTime(
                branch.getUpdateAt(),
                branch.getUpdatedTimeOffset() != null ? branch.getUpdatedTimeOffset() : branch.getCreatedTimeOffset()
        ));
        return dto;
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "USD";
        }
        return currency.trim().toUpperCase();
    }
}
