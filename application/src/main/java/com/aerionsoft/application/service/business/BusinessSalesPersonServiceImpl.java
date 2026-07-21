package com.aerionsoft.application.service.business;

import com.aerionsoft.application.dto.salesperson.SalesPersonResponseDto;
import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.entity.BusinessSalesPerson;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.business.BusinessSalesPersonRepository;
import com.aerionsoft.application.repository.access.RoleAssignmentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessSalesPersonServiceImpl implements BusinessSalesPersonService {

    private final BusinessSalesPersonRepository businessSalesPersonRepository;
    private final BusinessRepository businessRepository;
    private final AdminUserRepository adminUserRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;

    @Override
    public List<SalesPersonResponseDto> getSalesPersons(Long businessId) {
        ensureBusinessExists(businessId);
        return businessSalesPersonRepository.findSalesPersonsByBusinessId(businessId)
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addSalesPerson(Long businessId, Long salesPersonId) {
        if (businessSalesPersonRepository.existsByBusinessIdAndSalesPersonId(businessId, salesPersonId)) {
            return;
        }

        BusinessEntity business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", businessId));
        AdminUser salesPerson = getSalesPersonOrThrow(salesPersonId);

        businessSalesPersonRepository.save(
                BusinessSalesPerson.builder()
                        .business(business)
                        .salesPerson(salesPerson)
                        .build()
        );
    }

    @Override
    @Transactional
    public void removeSalesPerson(Long businessId, Long salesPersonId) {
        businessSalesPersonRepository.deleteByBusinessIdAndSalesPersonId(businessId, salesPersonId);
    }

    @Override
    @Transactional
    public void setSalesPersons(Long businessId, List<Long> salesPersonIds) {
        List<BusinessSalesPerson> existing = businessSalesPersonRepository.findByBusinessId(businessId);
        businessSalesPersonRepository.deleteAll(existing);

        if (salesPersonIds == null || salesPersonIds.isEmpty()) {
            return;
        }

        BusinessEntity business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", businessId));

        List<BusinessSalesPerson> toSave = salesPersonIds.stream()
                .distinct()
                .map(id -> BusinessSalesPerson.builder()
                        .business(business)
                        .salesPerson(getSalesPersonOrThrow(id))
                        .build())
                .toList();

        businessSalesPersonRepository.saveAll(toSave);
    }

    private void ensureBusinessExists(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new ResourceNotFoundException("Business", businessId);
        }
    }

    private AdminUser getSalesPersonOrThrow(Long salesPersonId) {
        AdminUser salesPerson = adminUserRepository.findById(salesPersonId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales person", salesPersonId));

        if (!roleAssignmentRepository.findAdminEntityIdsByRoleSlugOrName(
                SalesPersonService.SALES_PERSON_ROLE_SLUG,
                SalesPersonService.SALES_PERSON_ROLE_NAME).contains(salesPersonId)) {
            throw ServiceExceptions.business("User is not a sales person");
        }

        return salesPerson;
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
