package com.aerionsoft.application.dto.visa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisaApplicationRequest {
    private Long visaId;

    private Map<String, Object> answers;
}
