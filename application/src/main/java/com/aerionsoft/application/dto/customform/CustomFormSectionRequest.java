package com.aerionsoft.application.dto.customform;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomFormSectionRequest {
    private Long id;

    @JsonAlias("sectionTitle")
    private String title;

    @JsonAlias("sectionDescription")
    private String description;

    @JsonAlias("sortOrder")
    private Integer orderIndex;

    private List<CustomFormFieldRequest> fields;
}
