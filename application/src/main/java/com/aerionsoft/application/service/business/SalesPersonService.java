package com.aerionsoft.application.service.business;

import com.aerionsoft.application.util.UserDateTimeUtil;
import com.aerionsoft.application.service.access.RoleService;

import com.aerionsoft.application.dto.rolepermission.AssignRoleDto;
import com.aerionsoft.application.dto.salesperson.SalesPersonDto;
import com.aerionsoft.application.dto.salesperson.SalesPersonResponseDto;
import com.aerionsoft.application.dto.salesperson.UpdateSalesPersonRequest;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.access.RoleAssignmentRepository;
import com.aerionsoft.application.util.EmailUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesPersonService {

    public static final String SALES_PERSON_ROLE_SLUG = "sales-person";
    public static final String SALES_PERSON_ROLE_NAME = "Sales Person";

    private final AdminUserRepository adminUserRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    @Transactional
    public Long createSalesPerson(SalesPersonDto request) {
        String email = EmailUtils.normalize(request.getEmail());

        if (adminUserRepository.findByEmail(email).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "User already exists!");
        }

        AdminUser user = AdminUser.builder()
                .fullName(request.getFullName())
                .email(email)
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .address(request.getAddress())
                .image(request.getImage())
                .currency(request.getCurrency().toUpperCase())
                .isActive(true)
                .isVerified(true)
                .createdAt(UserDateTimeUtil.now())
                .build();

        adminUserRepository.save(user);

        AssignRoleDto assignRoleDto = new AssignRoleDto();
        assignRoleDto.setId(user.getId());
        assignRoleDto.setEntity("ADMIN");
        assignRoleDto.setRole(SALES_PERSON_ROLE_SLUG);
        roleService.assignRoleToUser("admin", user.getId(), assignRoleDto);

        return user.getId();
    }

    public List<SalesPersonResponseDto> getAllSalesPersons(String currencyCode) {
        String currency = (currencyCode == null || currencyCode.isBlank())
                ? null
                : currencyCode.toUpperCase();

        List<Long> salesPersonIds = roleAssignmentRepository.findAdminEntityIdsByRoleSlugOrName(
                SALES_PERSON_ROLE_SLUG, SALES_PERSON_ROLE_NAME);

        if (salesPersonIds.isEmpty()) {
            return List.of();
        }

        return adminUserRepository.findAllById(salesPersonIds)
                .stream()
                .filter(user -> user.getIsActive() == null || Boolean.TRUE.equals(user.getIsActive()))
                .filter(user -> currency == null
                        || user.getCurrency() == null
                        || user.getCurrency().isBlank()
                        || currency.equalsIgnoreCase(user.getCurrency()))
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    public SalesPersonResponseDto getSalesPersonById(Long id) {
        AdminUser user = getSalesPersonOrThrow(id);
        return toResponseDto(user);
    }

    @Transactional
    public void updateSalesPerson(Long id, UpdateSalesPersonRequest request) {
        AdminUser user = getSalesPersonOrThrow(id);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null) {
            String email = EmailUtils.normalize(request.getEmail());
            if (!email.equalsIgnoreCase(user.getEmail())
                    && adminUserRepository.findByEmail(email).isPresent()) {
                throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Email already exists!");
            }
            user.setEmail(email);
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getCurrency() != null) {
            user.setCurrency(request.getCurrency().toUpperCase());
        }
        if (request.getImage() != null) {
            user.setImage(request.getImage());
        }

        adminUserRepository.save(user);
    }

    @Transactional
    public void deleteSalesPerson(Long id) {
        AdminUser user = getSalesPersonOrThrow(id);
        user.setIsActive(false);
        user.setIsVerified(false);
        adminUserRepository.save(user);
    }

    private AdminUser getSalesPersonOrThrow(Long id) {
        AdminUser user = adminUserRepository.findById(id)
                .orElseThrow(() -> ServiceExceptions.notFound("Sales person not found!"));

        if (!roleAssignmentRepository.findAdminEntityIdsByRoleSlugOrName(
                SALES_PERSON_ROLE_SLUG, SALES_PERSON_ROLE_NAME).contains(id)) {
            throw ServiceExceptions.business("User is not a sales person");
        }

        return user;
    }

    private SalesPersonResponseDto toResponseDto(AdminUser user) {
        SalesPersonResponseDto dto = new SalesPersonResponseDto();
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAddress(user.getAddress());
        dto.setImage(user.getImage());
        return dto;
    }
}
