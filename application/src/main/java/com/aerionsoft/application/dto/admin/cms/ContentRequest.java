package com.aerionsoft.application.dto.admin.cms;

import com.aerionsoft.application.enums.cms.ContentStatus;
import com.aerionsoft.application.enums.cms.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentRequest {
    
    @NotNull(message = "Content type is required")
    private ContentType type;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Slug is required")
    private String slug;
    
    private String description;
    
    private String imageUrl;
    
    private String metaTitle;
    
    private String metaDescription;
    
    @NotNull(message = "Status is required")
    private ContentStatus status;
    
    private List<SectionRequest> sections;
    
    private Long categoryId;
    
    private Long tagId;
}
