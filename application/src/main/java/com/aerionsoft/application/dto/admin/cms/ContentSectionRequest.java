package com.aerionsoft.application.dto.admin.cms;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentSectionRequest {
    
    private Long sectionId;
    
    private Integer sortOrder;
}
