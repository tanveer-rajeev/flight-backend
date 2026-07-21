package com.aerionsoft.application.dto.breakingnews;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BreakingNewsRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String content;

    @NotNull(message = "Target audience is required")
    private String target;

    /** Higher number = shown first. Defaults to 0. */
    private Integer priority;

    private boolean active = true;

    /** ISO datetime — news is hidden before this point */
    private LocalDateTime startsAt;

    /** ISO datetime — news is hidden after this point */
    private LocalDateTime expiresAt;

    private String imageUrl;


    /** Optional deep-link */
    private String linkUrl;
}

