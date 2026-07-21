package com.aerionsoft.application.service.tour;

import com.aerionsoft.application.enums.notification.NotificationPriority;
import com.aerionsoft.application.enums.notification.NotificationType;
import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.tour.TourApplicationRequest;
import com.aerionsoft.application.dto.tour.TourApplicationResponse;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.cms.CustomForm;
import com.aerionsoft.application.entity.cms.CustomFormField;
import com.aerionsoft.application.entity.cms.CustomFormSection;
import com.aerionsoft.application.entity.tour.TourApplication;
import com.aerionsoft.application.entity.tour.TourPackage;
import com.aerionsoft.application.enums.tour.ApplicationStatus;
import com.aerionsoft.application.repository.cms.CustomFormRepository;
import com.aerionsoft.application.repository.tour.TourApplicationRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.tour.TourPackageRepository;
import com.aerionsoft.application.service.notification.NotificationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TourApplicationService {

    @Autowired
    private TourApplicationRepository tourApplicationRepository;

    @Autowired
    private CustomFormRepository customFormRepository;

    @Autowired
    private TourPackageRepository tourPackageRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationHelper notificationHelper;


    public List<TourApplicationResponse> getMyTourPackages(long userId) {
        List<TourApplication> applications = tourApplicationRepository.findByCreatedBy(String.valueOf(userId));
        return applications.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TourApplicationResponse createApplicationTour(TourApplicationRequest request, Long userId) {
        // Basic validation
        if (request.getTourId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Tour ID is required");
        }

        if (request.getFormResponses() == null || request.getFormResponses().isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Form responses cannot be empty");
        }

        // Get the tour package and extract fromId
        TourPackage tourPackage = tourPackageRepository.findById(request.getTourId())
                .orElseThrow(() -> new ResourceNotFoundException("Tour package", request.getTourId()));

        if (tourPackage.getFromId() == null) {
            throw ServiceExceptions.notFound("Tour package does not have an associated form");
        }

        // Get the actual form and validate against it
        CustomForm customForm = customFormRepository.findById(tourPackage.getFromId())
                .orElseThrow(() -> new ResourceNotFoundException("Form", tourPackage.getFromId()));


        // Validate that the provided form responses match the form fields
        validateFormFields(customForm, request.getFormResponses());

        TourApplication application = new TourApplication();
        application.setFormId(tourPackage.getFromId()); // Use fromId from tour package
        application.setTourId(request.getTourId());
        application.setFormResponses(request.getFormResponses());
        application.setStatus(ApplicationStatus.PENDING);
        application.setSubmittedAt(UserDateTimeUtil.now());
        application.setCreatedBy(String.valueOf(userId));

        if (request.getRemarks() != null && !request.getRemarks().trim().isEmpty()) {
            application.setRemarks(request.getRemarks());
        }

        try {
            TourApplication savedApplication = tourApplicationRepository.save(application);
            return toResponse(savedApplication);
        } catch (Exception ex) {
            throw ServiceExceptions.notFound("Error saving tour application: " + ex.getMessage());
        }

    }

    private void validateFormFields(CustomForm customForm, Map<String, Object> formResponses) {
        if (formResponses == null || formResponses.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Form responses cannot be empty");
        }

        // Collect all valid field names from the form
        Set<String> validFieldNames = new HashSet<>();
        Set<String> requiredFieldNames = new HashSet<>();

        if (customForm.getSections() != null) {
            for (CustomFormSection section : customForm.getSections()) {
                if (section.getFields() != null) {
                    for (CustomFormField field : section.getFields()) {
                        validFieldNames.add(field.getFieldName());
                        if (field.getIsRequired() != null && field.getIsRequired()) {
                            requiredFieldNames.add(field.getFieldName());
                        }
                    }
                }
            }
        }

        // Check for invalid field names in responses
        for (String responseKey : formResponses.keySet()) {
            if (!validFieldNames.contains(responseKey)) {
                throw ServiceExceptions.notFound("Invalid field name: " + responseKey + " does not exist in form: " + customForm.getTitle());
            }
        }

        // Check for missing required fields
        for (String requiredField : requiredFieldNames) {
            if (!formResponses.containsKey(requiredField) || formResponses.get(requiredField) == null ||
                    (formResponses.get(requiredField) instanceof String && ((String) formResponses.get(requiredField)).trim().isEmpty())) {
                throw ServiceExceptions.validation("Required field '" + requiredField + "' is missing or empty");
            }
        }
    }

    public TourApplicationResponse getApplicationById(Long id) {
        TourApplication application = tourApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour application", id));
        return toResponse(application);
    }

    public List<TourApplicationResponse> getAllTourApplications() {
        return tourApplicationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public TourApplicationResponse updateApplicationStatus(Long id, ApplicationStatus status, String remarks) {
        TourApplication application = tourApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour application", id));

        // Set status directly since TourApplication uses ApplicationStatus enum
        application.setStatus(status);
        application.setProcessedAt(UserDateTimeUtil.now());
        if (remarks != null && !remarks.trim().isEmpty()) {
            application.setRemarks(remarks);
        }

        TourApplication updated = tourApplicationRepository.save(application);

        // Notify user about status update
        try {
            Long userId = Long.parseLong(application.getCreatedBy());
            TourPackage tourPackage = tourPackageRepository.findById(application.getTourId()).orElse(null);
            String packageName = tourPackage != null ? tourPackage.getTitle() : "Tour Package";

            notificationHelper.sendCustomNotification(
                    userId,
                    NotificationType.TOUR_APPLICATION_UPDATE,
                    NotificationPriority.MEDIUM,
                    "Tour Application Update",
                    "Your tour application for " + packageName + " status has been updated to " + status,
                    "/tour/applications/" + application.getId(),
                    "View Application"
            );
        } catch (Exception e) {
            // ignore
        }

        return toResponse(updated);
    }

    public void deleteApplication(Long id) {
        if (!tourApplicationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tour application", id);
        }
        tourApplicationRepository.deleteById(id);
    }

    public TourApplicationResponse toResponse(TourApplication entity) {
        if (entity == null) return null;
        TourApplicationResponse dto = new TourApplicationResponse();
        dto.setId(entity.getId());
        dto.setFormId(entity.getFormId());
        dto.setTourId(entity.getTourId());
        dto.setFormResponses(entity.getFormResponses());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setSubmittedAt(entity.getSubmittedAt());
        dto.setProcessedAt(entity.getProcessedAt());
        dto.setProcessedBy(entity.getProcessedBy());
        dto.setRemarks(entity.getRemarks());

        dto.setCreatedBy(entity.getCreatedBy());
        if (entity.getCreatedBy() == null) {
            dto.setCreatedByName(null);
            return dto;
        }

        User user = userRepository.getById(Long.valueOf(entity.getCreatedBy()));
        dto.setCreatedByName(user.getFullName());


        return dto;
    }

    // Add Specification-based filtering method like visa applications
    public List<TourApplicationResponse> getAllApplicationsWithAdvancedFilters(
            ApplicationStatus status,
            Long tourId,
            Long formId,
            LocalDateTime submittedFrom,
            LocalDateTime submittedTo,
            String processedBy) {

        Specification<TourApplication> spec = Specification.anyOf();

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (tourId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tourId"), tourId));
        }
        if (formId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("formId"), formId));
        }
        if (submittedFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("submittedAt"), submittedFrom));
        }
        if (submittedTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("submittedAt"), submittedTo));
        }
        if (processedBy != null && !processedBy.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("processedBy")),
                    "%" + processedBy.toLowerCase() + "%"));
        }

        // Add ordering by submitted date descending
        List<TourApplication> applications = tourApplicationRepository.findAll(spec,
                Sort.by(Sort.Direction.DESC, "submittedAt"));

        return applications.stream()
                .map(this::toResponse)
                .toList();
    }
}
