package com.aerionsoft.application.dto.client.invoice.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LedgerResponseDto {
    private Long id;
    private String title;
    private String image;
    private String description;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
