package com.aerionsoft.application.dto.customform;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomFormFieldRequest {
    private Long id;
    private String fieldName;
    private String label;
    private String fieldType;
    private String placeholder;
    private Boolean isRequired;
    private List<String> options;
    private Integer sortOrder;
}
