package com.aerionsoft.application.service.business;

import com.aerionsoft.application.dto.business.BusinessDto;
import com.aerionsoft.application.dto.business.BusinessRequest;
import com.aerionsoft.application.dto.business.PublicAgencyRequest;
import com.aerionsoft.application.dto.UpdateBusinessRequest;
import com.aerionsoft.application.dto.UpdateBusinessStatusRequest;
import com.aerionsoft.application.dto.client.user.UserDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BusinessService {
    BusinessDto createBusiness(BusinessRequest request);
    BusinessDto requestBusiness(BusinessRequest request);

    /**
     * Public agency signup: creates a mother user from representative details
     * and a business in PENDING status (no motherUserId required).
     */
    BusinessDto createPublicAgency(PublicAgencyRequest request);
    BusinessDto approveBusiness(Long businessId);
    BusinessDto updateBusinessStatus(Long businessId, UpdateBusinessStatusRequest request);
    BusinessDto assignMotherUser(Long businessId, Long userId);
    BusinessDto updateBusiness(Long businessId, UpdateBusinessRequest request);
    BusinessDto getBusinessById(Long businessId);
    List<UserDto> getUsersOfBusiness(Long businessId);
    Page<BusinessDto> getAllBusinesses(String currency, String query, int page, int size);
    BusinessDto getBusinessByUserId(Long userId);

    /**
     * Permanently deletes a REJECTED agency and all data owned by its users.
     * Throws an exception if the agency is not in REJECTED status.
     */
    void deleteRejectedAgency(Long businessId);
}
