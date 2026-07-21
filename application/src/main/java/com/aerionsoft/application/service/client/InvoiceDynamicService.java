package com.aerionsoft.application.service.client;

import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.client.invoice.InvoiceDynamicServiceDto;
import com.aerionsoft.application.dto.client.invoice.ServiceKeyStepDto;
import com.aerionsoft.application.dto.client.invoice.response.ServiceKeyStepResponseDto;
import com.aerionsoft.application.entity.client.DynamicService;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.enums.client.InvoiceType;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.client.InvoiceDynamicServiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class InvoiceDynamicService {

    private final InvoiceDynamicServiceRepository invoiceDynamicServiceRepository;
    private final UserRepository userRepository;

    public InvoiceDynamicService(InvoiceDynamicServiceRepository invoiceDynamicServiceRepository, UserRepository userRepository) {
        this.invoiceDynamicServiceRepository = invoiceDynamicServiceRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get Dynamic Services by Grouped
     *
     * @param provider admin or user
     * @param authUserId auth user id
     * @return data as key value pair (Map)
     */
    public Map<String, List<ServiceKeyStepResponseDto>> getDynamicServicesGrouped(String provider, Long authUserId) {
        if (Objects.equals(provider, "admin")) {
            List<DynamicService> dynamicServices = invoiceDynamicServiceRepository.findByAgencyUserIsNull();

            return dynamicServices.stream()
                    .collect(Collectors.groupingBy(
                            ds -> ds.getServiceType().name(),   // key: "TICKET", "VISA", ...
                            Collectors.mapping(ds -> new ServiceKeyStepResponseDto(ds.getId(), ds.getServiceKey(), ds.getStep()), Collectors.toList())
                    ));
        }

        User user = userRepository.findById(authUserId).orElseThrow(() -> new ResourceNotFoundException("User"));
        User parentUser = user.getParentUser() != null ? user.getParentUser() : user;

        List<DynamicService> dynamicServices = invoiceDynamicServiceRepository.findByAgencyUser(parentUser);

        return dynamicServices.stream()
                .collect(Collectors.groupingBy(
                        ds -> ds.getServiceType().name(),   // key: "TICKET", "VISA", ...
                        Collectors.mapping(ds -> new ServiceKeyStepResponseDto(ds.getId(), ds.getServiceKey(), ds.getStep()), Collectors.toList())
                ));
    }

    public List<ServiceKeyStepResponseDto> getDynamicServiceByServiceKey(String provider, Long authUserId, String serviceType) {
        if (Objects.equals(provider, "admin")) {
            List<DynamicService> dynamicServices = invoiceDynamicServiceRepository.findByServiceType(InvoiceType.valueOf(serviceType));

            return dynamicServices.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }

        User user = userRepository.findById(authUserId).orElseThrow(() -> new ResourceNotFoundException("User"));
        User parentUser = user.getParentUser() != null ? user.getParentUser() : user;

        List<DynamicService> dynamicServices = invoiceDynamicServiceRepository.findByServiceTypeAndAgencyUser(InvoiceType.valueOf(serviceType), parentUser);

        return dynamicServices.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Store InvoiceDynamicService
     *
     * @param provider admin or user
     * @param authUserId auth user id
     * @param dto request data to create dynamic service
     */
    public void createDynamicService(String provider, Long authUserId, InvoiceDynamicServiceDto dto) {
        if (Objects.equals(provider, "admin")) {
            for (ServiceKeyStepDto keyDto : dto.getKeys()) {
                // Checking key already exist on for current ServiceType
                DynamicService isExist = invoiceDynamicServiceRepository
                        .findByServiceKeyAndServiceType(keyDto.getServiceKey(), InvoiceType.valueOf(dto.getServiceType()))
                        .orElse(null);

                if (Objects.isNull(isExist)) {
                    DynamicService dynamicService = DynamicService
                            .builder()
                            .serviceKey(keyDto.getServiceKey())
                            .serviceType(InvoiceType.valueOf(dto.getServiceType()))
                            .step(keyDto.getStep())
                            .build();

                    invoiceDynamicServiceRepository.save(dynamicService);
                }
            }

        } else {
            User user = userRepository.findById(authUserId).orElseThrow(() -> new ResourceNotFoundException("User"));
            User parentUser = user.getParentUser() != null ? user.getParentUser() : user;

            for (ServiceKeyStepDto keyDto : dto.getKeys()) {
                // Checking key already exist on for current ServiceType
                DynamicService isExist = invoiceDynamicServiceRepository
                        .findByServiceKeyAndServiceTypeAndAgencyUser(keyDto.getServiceKey(), InvoiceType.valueOf(dto.getServiceType()), parentUser)
                        .orElse(null);

                if (Objects.isNull(isExist)) {
                    DynamicService dynamicService = DynamicService
                            .builder()
                            .agencyUser(parentUser)
                            .serviceKey(keyDto.getServiceKey())
                            .serviceType(InvoiceType.valueOf(dto.getServiceType()))
                            .step(keyDto.getStep())
                            .build();

                    invoiceDynamicServiceRepository.save(dynamicService);
                }
            }
        }
    }

    /**
     * Delete dynamic invoice item service
     *
     * @param provider admin or user
     * @param authUserId auth user id
     * @param id item id
     */
    public void deleteDynamicService(String provider, Long authUserId, Long id) {
        if (Objects.equals(provider, "admin")) {
            DynamicService dynamicService = invoiceDynamicServiceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Item"));
            invoiceDynamicServiceRepository.delete(dynamicService);
        } else {
            User user = userRepository.findById(authUserId).orElseThrow(() -> new ResourceNotFoundException("User"));
            User parentUser = user.getParentUser() != null ? user.getParentUser() : user;

            DynamicService dynamicService = invoiceDynamicServiceRepository.findByIdAndAgencyUser(id, parentUser).orElseThrow(() -> new ResourceNotFoundException("Item"));

            invoiceDynamicServiceRepository.delete(dynamicService);
        }
    }

    private ServiceKeyStepResponseDto toDto(DynamicService dynamicService) {
        ServiceKeyStepResponseDto serviceKeyStepResponseDto = new ServiceKeyStepResponseDto();
        serviceKeyStepResponseDto.setId(dynamicService.getId());
        serviceKeyStepResponseDto.setServiceKey(dynamicService.getServiceKey());
        serviceKeyStepResponseDto.setStep(dynamicService.getStep());

        return serviceKeyStepResponseDto;
    }
}
