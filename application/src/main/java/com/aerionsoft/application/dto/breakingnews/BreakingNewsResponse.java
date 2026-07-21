package com.aerionsoft.application.dto.breakingnews;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BreakingNewsResponse {

    private Long id;
    private String title;
    private String content;
    private String target;
    private Integer priority;
    private boolean active;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private String linkUrl;
    private Long createdBy;
    private String createdByName;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

