package com.aerionsoft.application.dto;

import lombok.Data;

@Data
public class SupplierDto {

    private Long id;
    private Long agencyId;        // Extract agency user ID only (avoid User entity)
    private String name;
    private String title;
    private String description;
    private String email;
    private String address;
    private String phoneNumber;
    private Long createdBy;
    private Long updatedBy;
    private String createAt;      // ISO date string
    private String updateAt;      // ISO date string
    private Boolean isDeleted;
}
