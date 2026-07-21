package com.aerionsoft.application.dto.visa;

import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisaApplicationDTO {
    private Long formId;
    private Long visaId;
    private VisaApplicantDTO applicant;
    private Map<String, String> answers;
    private List<VisaDocumentDTO> documents;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VisaApplicantDTO {
        private String fullName;
        private String passportNumber;
        private String nationality;
        private LocalDate dateOfBirth;
        private String email;
        private String phone;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VisaDocumentDTO {
        private String docType;
        private String url;
    }
}
