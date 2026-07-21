package com.aerionsoft.application.dto.tour;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourPackageTypeRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    private String description;

    private Boolean isActive = true;
}
