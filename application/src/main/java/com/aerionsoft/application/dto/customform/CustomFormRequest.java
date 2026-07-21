package com.aerionsoft.application.dto.customform;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomFormRequest {
    @NotBlank(message = "Title is required")
    private String title;
    private String bannerImage;
    private String description;
    private List<CustomFormSectionRequest> sections;
}
