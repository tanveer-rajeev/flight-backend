package com.aerionsoft.application.dto.admin.cms;

import com.aerionsoft.application.enums.cms.ContentType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageListItemResponse {
    
    private String title;
    
    private String slug;
    
    private ContentType type;
}

