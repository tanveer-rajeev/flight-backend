package com.aerionsoft.application.dto.client.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private Long id;
    private String type;
    private Double amount;
    private String currency;
    private String convertedAmount;
    private String description;
    private Long userId;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private String sourceType;
    private Long sourceId;
    private SourceSummary sourceSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceSummary {
        private String label;
        private String detail;
        private String status;
    }
}

