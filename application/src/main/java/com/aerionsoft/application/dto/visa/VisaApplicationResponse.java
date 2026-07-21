package com.aerionsoft.application.dto.visa;

import com.aerionsoft.application.enums.tour.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisaApplicationResponse {
    private String visaType;
    private String visaCountry;
    private Long id;
    private String formTitle;
    private List<SectionResponse> sections;
    private ApplicationStatus applicationStatus;
    private LocalDateTime submittedAt;
    private LocalDateTime processedAt;
    private String processedBy;
    private String remarks;
    private String createdBy;
    private String createdByName;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionResponse {
        private String sectionTitle;
        private Map<String, Object> fields;
    }
}
