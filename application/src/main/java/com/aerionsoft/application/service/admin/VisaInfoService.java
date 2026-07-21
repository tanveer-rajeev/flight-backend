package com.aerionsoft.application.service.admin;

import com.aerionsoft.application.dto.visa.VisaInfoRequest;
import com.aerionsoft.application.dto.visa.VisaInfoResponse;

import java.util.List;

public interface VisaInfoService {
    VisaInfoResponse createVisaInfo(VisaInfoRequest request);
    VisaInfoResponse getVisaInfoById(Long id);
    List<VisaInfoResponse> getAllVisaInfo();
    List<VisaInfoResponse> getVisaInfoByCountry(String country);
    VisaInfoResponse updateVisaInfo(Long id, VisaInfoRequest request);
    void deleteVisaInfo(Long id);
}
