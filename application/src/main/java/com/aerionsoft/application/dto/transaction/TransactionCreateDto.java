package com.aerionsoft.application.dto.transaction;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
public class TransactionCreateDto {
    private double amount;
    private String convertedAmount;
    private String currency;
    private String description;
    private String type;
    private Long userId;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String sourceType;
    private Long sourceId;
    private String reference;
    private String paymentId;
}
