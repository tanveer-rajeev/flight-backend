package com.aerionsoft.application.dto.client.branch;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BranchResponseDto {

    private Long id;
    private Long agencyId;
    private String name;
    private String description;
    private String address;
    private String phoneNumber;
    private String currency;
    private Boolean isActive;
    private Boolean isDeleted;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
