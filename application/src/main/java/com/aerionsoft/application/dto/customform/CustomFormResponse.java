package com.aerionsoft.application.dto.customform;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomFormResponse {
    private Long id;
    private String title;
    private String slug;
    private String bannerImage;
    private String description;
    private Integer formStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CustomFormSectionResponse> sections;
}
