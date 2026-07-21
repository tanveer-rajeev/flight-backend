package com.aerionsoft.application.service.visa;

import com.aerionsoft.application.dto.visa.VisaApplicationDetailResponse;
import com.aerionsoft.application.dto.visa.VisaApplicationRequest;
import com.aerionsoft.application.dto.visa.VisaApplicationResponse;
import com.aerionsoft.application.enums.tour.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface VisaApplicationService {
    VisaApplicationResponse createApplication(VisaApplicationRequest request,Long userId);
    VisaApplicationResponse getApplicationById(Long id);
    VisaApplicationDetailResponse getApplicationDetailById(Long id);
    List<VisaApplicationResponse> getAllApplications();
    List<VisaApplicationResponse> getAllApplicationsWithFilters(
            ApplicationStatus status,
            String visaType,
            String country,
            LocalDateTime submittedFrom,
            LocalDateTime submittedTo,
            String processedBy,
            Long visaId
    );
    VisaApplicationResponse updateApplicationStatus(Long id, ApplicationStatus status, String remarks);
    void deleteApplication(Long id);
    List<VisaApplicationResponse>  getMyVisaApplications(Long userId);
}
