package com.aerionsoft.application.dto.visa;

import com.aerionsoft.application.dto.customform.CustomFormResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VisaApplicationDetailResponse extends VisaApplicationResponse {
    private Long visaId;
    private Long formId;
    private LocalDateTime createdAt;
    private Map<String, Object> formResponses;
    private VisaInfoResponse visaInfo;
    private CustomFormResponse form;
    private List<VisaPaymentInfoResponse> payments;
    private List<VisaApplicationTimelineEvent> timeline;
}
