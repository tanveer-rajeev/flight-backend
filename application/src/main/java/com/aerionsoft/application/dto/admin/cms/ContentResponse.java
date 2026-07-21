package com.aerionsoft.application.dto.admin.cms;

import com.aerionsoft.application.enums.cms.ContentStatus;
import com.aerionsoft.application.enums.cms.ContentType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentResponse {
    
    private UUID id;
    
    private ContentType type;
    
    private String title;
    
    private String slug;
    
    private String description;
    
    private String imageUrl;
    
    private String metaTitle;
    
    private String metaDescription;
    
    private ContentStatus status;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private List<SectionResponse> sections;
    
    private CategoryResponse category;
    
    private TagResponse tag;
}
