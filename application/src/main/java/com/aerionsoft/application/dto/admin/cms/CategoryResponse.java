package com.aerionsoft.application.dto.admin.cms;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {
    
    private Long id;
    
    private String title;
    
    private String description;
}
