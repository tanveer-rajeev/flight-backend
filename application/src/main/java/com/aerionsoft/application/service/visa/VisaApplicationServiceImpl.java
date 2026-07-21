package com.aerionsoft.application.service.visa;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.visa.VisaApplicationDetailResponse;
import com.aerionsoft.application.dto.visa.VisaApplicationRequest;
import com.aerionsoft.application.dto.visa.VisaApplicationResponse;
import com.aerionsoft.application.dto.visa.VisaApplicationTimelineEvent;
import com.aerionsoft.application.dto.visa.VisaInfoResponse;
import com.aerionsoft.application.dto.visa.VisaPaymentInfoResponse;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.cms.CustomForm;
import com.aerionsoft.application.entity.cms.CustomFormField;
import com.aerionsoft.application.entity.cms.CustomFormSection;
import com.aerionsoft.application.entity.visa.VisaApplication;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.tour.ApplicationStatus;
import com.aerionsoft.application.enums.wallet.TransactionSourceType;
import com.aerionsoft.application.repository.cms.CustomFormRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.visa.VisaApplicationRepository;
import com.aerionsoft.application.repository.wallet.TransactionRepository;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import com.aerionsoft.application.service.admin.CustomFormService;
import com.aerionsoft.application.service.admin.VisaInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VisaApplicationServiceImpl implements VisaApplicationService {

    @Autowired
    private VisaApplicationRepository visaApplicationRepository;

    @Autowired
    private CustomFormRepository customFormRepository;

    @Autowired
    private VisaInfoService visaInfoService;

    @Autowired
    private CustomFormService customFormService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletDepositRepository walletDepositRepository;

    @Autowired
    UserRepository userRepository;

    public List<VisaApplicationResponse> getMyVisaApplications(Long userId) {
        // Convert userId to String since created_by is a character varying in the database
        String userIdStr = userId.toString();
        List<VisaApplication> applications = visaApplicationRepository.findByCreatedBy(userIdStr);
        return applications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public VisaApplicationResponse createApplication(VisaApplicationRequest request, Long userId) {
        // Validate visa exists and get its form ID
        VisaInfoResponse visaInfo = visaInfoService.getVisaInfoById(request.getVisaId());

        if (visaInfo.getFormId() == null) {
            throw ServiceExceptions.notFound("No form is associated with visa ID: " + request.getVisaId());
        }

        // Get the form using the form ID from visa info
        CustomForm customForm = customFormRepository.findById(visaInfo.getFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Form", visaInfo.getFormId()));

        // Validate that the provided answers match the form fields
        validateFormFields(customForm, request.getAnswers());

        VisaApplication application = new VisaApplication();

        // Set basic info - use form ID from visa info
        application.setFormId(visaInfo.getFormId());
        application.setVisaId(request.getVisaId());

        // Store form responses (all data including applicant info will be in form responses)
        application.setFormResponses(request.getAnswers());
        application.setStatus(ApplicationStatus.PENDING);
        application.setSubmittedAt(UserDateTimeUtil.now());
        application.setCreatedBy(userId.toString()); // Store userId as String
        VisaApplication savedApplication = visaApplicationRepository.save(application);
        return mapToResponse(savedApplication);
    }

    private void validateFormFields(CustomForm customForm, Map<String, Object> answers) {
        if (answers == null || answers.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Form answers cannot be empty");
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

        // Check for invalid field names in answers
        for (String answerKey : answers.keySet()) {
            if (!validFieldNames.contains(answerKey)) {
                throw ServiceExceptions.notFound("Invalid field name: " + answerKey + " does not exist in form: " + customForm.getTitle());
            }
        }

        // Check for missing required fields
        for (String requiredField : requiredFieldNames) {
            if (!answers.containsKey(requiredField) || answers.get(requiredField) == null ||
                    (answers.get(requiredField) instanceof String && ((String) answers.get(requiredField)).trim().isEmpty())) {
                throw ServiceExceptions.validation("Required field '" + requiredField + "' is missing or empty");
            }
        }
    }

    @Override
    public VisaApplicationResponse getApplicationById(Long id) {
        VisaApplication application = visaApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visa application", id));
        return mapToResponse(application);
    }

    @Override
    public VisaApplicationDetailResponse getApplicationDetailById(Long id) {
        VisaApplication application = visaApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visa application", id));

        VisaApplicationDetailResponse detail = new VisaApplicationDetailResponse();
        copyApplicationFields(application, detail);

        detail.setVisaId(application.getVisaId());
        detail.setFormId(application.getFormId());
        detail.setCreatedAt(application.getCreatedAt());
        detail.setFormResponses(application.getFormResponses());

        detail.setVisaInfo(visaInfoService.getVisaInfoById(application.getVisaId()));
        detail.setForm(customFormService.getFormById(application.getFormId()));

        if (detail.getVisaInfo() != null) {
            detail.setVisaType(detail.getVisaInfo().getVisaType());
            detail.setVisaCountry(detail.getVisaInfo().getCountry());
        }
        if (detail.getForm() != null) {
            detail.setFormTitle(detail.getForm().getTitle());
        }

        List<VisaPaymentInfoResponse> payments = buildPaymentInfo(application.getId());
        detail.setPayments(payments);
        detail.setTimeline(buildTimeline(application, payments));

        return detail;
    }

    @Override
    public List<VisaApplicationResponse> getAllApplications() {
        List<VisaApplication> applications = visaApplicationRepository.findAll();
        return applications.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<VisaApplicationResponse> getAllApplicationsWithFilters(
            ApplicationStatus status,
            String visaType,
            String country,
            LocalDateTime submittedFrom,
            LocalDateTime submittedTo,
            String processedBy,
            Long visaId) {

        Specification<VisaApplication> spec = Specification.anyOf();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (visaType != null && !visaType.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("visaInfo").get("visaType"), visaType));
        }
        if (country != null && !country.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("visaInfo").get("country"), country));
        }
        if (submittedFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("submittedAt"), submittedFrom));
        }
        if (submittedTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("submittedAt"), submittedTo));
        }
        if (processedBy != null && !processedBy.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("processedBy"), processedBy));
        }
        if (visaId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("visaId"), visaId));
        }
        // Add ordering by submitted date descending
        List<VisaApplication> applications = visaApplicationRepository.findAll(spec,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "submittedAt"));

        return applications.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public VisaApplicationResponse updateApplicationStatus(Long id, ApplicationStatus status, String remarks) {
        Optional<VisaApplication> applicationOpt = visaApplicationRepository.findById(id);
        if (applicationOpt.isPresent()) {
            VisaApplication application = applicationOpt.get();
            application.setStatus(status);
            if (remarks != null) {
                application.setRemarks(remarks);
            }
            if (status == ApplicationStatus.APPROVED || status == ApplicationStatus.REJECTED) {
                application.setProcessedAt(UserDateTimeUtil.now());
            }
            VisaApplication savedApplication = visaApplicationRepository.save(application);
            return mapToResponse(savedApplication);
        }
        throw new ResourceNotFoundException("Visa application", id);
    }

    @Override
    @Transactional
    public void deleteApplication(Long id) {
        if (visaApplicationRepository.existsById(id)) {
            visaApplicationRepository.deleteById(id);
        } else {
            throw new ResourceNotFoundException("Visa application", id);
        }
    }

    private VisaApplicationResponse mapToResponse(VisaApplication application) {
        VisaApplicationResponse response = new VisaApplicationResponse();
        copyApplicationFields(application, response);
        return response;
    }

    private void copyApplicationFields(VisaApplication application, VisaApplicationResponse response) {
        // Set visa type from VisaInfo
        if (application.getVisaInfo() != null) {
            response.setVisaType(application.getVisaInfo().getVisaType());
            response.setVisaCountry(application.getVisaInfo().getCountry());
        }

        response.setId(application.getId());
        response.setApplicationStatus(application.getStatus());
        response.setSubmittedAt(application.getSubmittedAt());
        response.setProcessedAt(application.getProcessedAt());
        response.setProcessedBy(application.getProcessedBy());
        response.setRemarks(application.getRemarks());

        // Get form details and structure the response
        if (application.getCustomForm() != null) {
            response.setFormTitle(application.getCustomForm().getTitle());
            response.setSections(buildSectionResponses(application));
        }
        response.setCreatedBy(application.getCreatedBy());
        if (application.getCreatedBy() == null) {
            response.setCreatedByName(null);
            return;
        }
        User user = userRepository.getById(Long.parseLong(application.getCreatedBy()));
        response.setCreatedByName(user.getFullName());
    }

    private List<VisaPaymentInfoResponse> buildPaymentInfo(Long applicationId) {
        return transactionRepository
                .findBySourceTypeAndSourceId(TransactionSourceType.VISA.name(), applicationId)
                .stream()
                .map(this::mapTransactionToPaymentInfo)
                .sorted(Comparator.comparing(
                        VisaPaymentInfoResponse::getPaidAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private VisaPaymentInfoResponse mapTransactionToPaymentInfo(Transaction transaction) {
        Long depositId = null;
        if (transaction.getReference() != null) {
            depositId = walletDepositRepository.findByReference(transaction.getReference())
                    .map(WalletDeposit::getId)
                    .orElse(null);
        }

        return VisaPaymentInfoResponse.builder()
                .transactionId(transaction.getId())
                .depositId(depositId)
                .reference(transaction.getReference())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .description(transaction.getDescription())
                .paidAt(transaction.getCreatedAt())
                .status(transaction.getStatus())
                .build();
    }

    private List<VisaApplicationTimelineEvent> buildTimeline(
            VisaApplication application,
            List<VisaPaymentInfoResponse> payments) {
        List<VisaApplicationTimelineEvent> timeline = new ArrayList<>();

        if (application.getCreatedAt() != null) {
            timeline.add(VisaApplicationTimelineEvent.builder()
                    .eventType("CREATED")
                    .title("Application created")
                    .description("Visa application was created")
                    .status(ApplicationStatus.PENDING)
                    .actorName(resolveUserName(application.getCreatedBy()))
                    .occurredAt(application.getCreatedAt())
                    .build());
        }

        if (application.getSubmittedAt() != null
                && !application.getSubmittedAt().equals(application.getCreatedAt())) {
            timeline.add(VisaApplicationTimelineEvent.builder()
                    .eventType("SUBMITTED")
                    .title("Application submitted")
                    .description("Visa application was submitted for processing")
                    .status(ApplicationStatus.PENDING)
                    .actorName(resolveUserName(application.getCreatedBy()))
                    .occurredAt(application.getSubmittedAt())
                    .build());
        }

        for (VisaPaymentInfoResponse payment : payments) {
            timeline.add(VisaApplicationTimelineEvent.builder()
                    .eventType("PAYMENT")
                    .title("Payment recorded")
                    .description(payment.getDescription() != null
                            ? payment.getDescription()
                            : "Visa application payment")
                    .actorName(resolveUserName(application.getCreatedBy()))
                    .occurredAt(payment.getPaidAt())
                    .build());
        }

        if (application.getProcessedAt() != null) {
            timeline.add(VisaApplicationTimelineEvent.builder()
                    .eventType("STATUS_UPDATED")
                    .title("Application " + formatStatus(application.getStatus()))
                    .description(application.getRemarks())
                    .status(application.getStatus())
                    .actorName(application.getProcessedBy())
                    .occurredAt(application.getProcessedAt())
                    .build());
        } else if (application.getStatus() != null && application.getStatus() != ApplicationStatus.PENDING) {
            timeline.add(VisaApplicationTimelineEvent.builder()
                    .eventType("STATUS_UPDATED")
                    .title("Status updated to " + formatStatus(application.getStatus()))
                    .description(application.getRemarks())
                    .status(application.getStatus())
                    .actorName(application.getProcessedBy())
                    .occurredAt(application.getSubmittedAt())
                    .build());
        }

        timeline.sort(Comparator.comparing(
                VisaApplicationTimelineEvent::getOccurredAt,
                Comparator.nullsLast(Comparator.naturalOrder())));

        return timeline;
    }

    private String resolveUserName(String userId) {
        if (userId == null) {
            return null;
        }
        try {
            return userRepository.findById(Long.parseLong(userId))
                    .map(User::getFullName)
                    .orElse(null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String formatStatus(ApplicationStatus status) {
        if (status == null) {
            return "updated";
        }
        return status.name().toLowerCase().replace('_', ' ');
    }

    private List<VisaApplicationResponse.SectionResponse> buildSectionResponses(VisaApplication application) {
        List<VisaApplicationResponse.SectionResponse> sections = new ArrayList<>();
        Map<String, Object> formResponses = application.getFormResponses();

        if (application.getCustomForm() != null && application.getCustomForm().getSections() != null) {
            // Sort sections by sort order
            List<CustomFormSection> sortedSections = application.getCustomForm().getSections().stream()
                    .sorted(Comparator.comparing(CustomFormSection::getSortOrder))
                    .toList();

            for (CustomFormSection section : sortedSections) {
                VisaApplicationResponse.SectionResponse sectionResponse = new VisaApplicationResponse.SectionResponse();
                sectionResponse.setSectionTitle(section.getSectionTitle());

                Map<String, Object> sectionFields = new LinkedHashMap<>(); // Use LinkedHashMap to maintain order

                // Sort fields by sort order and map form responses to field labels
                if (section.getFields() != null) {
                    List<CustomFormField> sortedFields = section.getFields().stream()
                            .sorted(Comparator.comparing(CustomFormField::getSortOrder))
                            .toList();

                    for (CustomFormField field : sortedFields) {
                        String fieldName = field.getFieldName();
                        String fieldLabel = field.getFieldLabel();

                        if (formResponses != null && formResponses.containsKey(fieldName)) {
                            sectionFields.put(fieldLabel, formResponses.get(fieldName));
                        }
                    }
                }

                sectionResponse.setFields(sectionFields);
                sections.add(sectionResponse);
            }
        }

        return sections;
    }
}
