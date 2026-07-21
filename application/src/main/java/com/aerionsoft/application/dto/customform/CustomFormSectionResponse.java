package com.aerionsoft.application.dto.customform;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomFormSectionResponse {
    private Long id;
    private String sectionTitle;
    private String sectionDescription;
    private Integer sortOrder;
    private List<CustomFormFieldResponse> fields;

    @JsonProperty("title")
    public String getTitle() {
        return sectionTitle;
    }

    @JsonProperty("description")
    public String getDescription() {
        return sectionDescription;
    }

    @JsonProperty("orderIndex")
    public Integer getOrderIndex() {
        return sortOrder;
    }
}
