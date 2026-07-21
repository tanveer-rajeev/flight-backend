package com.aerionsoft.application.dto.admin.cms;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    private String imageUrl;
}
