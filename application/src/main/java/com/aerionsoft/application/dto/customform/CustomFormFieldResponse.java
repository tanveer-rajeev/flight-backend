package com.aerionsoft.application.dto.customform;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomFormFieldResponse {
    private Long id;
    private String fieldName;
    private String label;
    private String fieldType;
    private Boolean isRequired;
    private List<String> options;
    private String placeholder;
    private Integer sortOrder;
}
