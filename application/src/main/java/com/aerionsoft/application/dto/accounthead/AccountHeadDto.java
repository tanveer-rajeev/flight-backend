package com.aerionsoft.application.dto.accounthead;

import lombok.Data;

@Data
public class AccountHeadDto {

    private Long id;
    private String accountHeadTitle;
    private String type;              // enum as String
    private Long parentId;
    private Long createdBy;
    private Long updatedBy;
    private String createdAt;         // return as String (ISO-8601)
    private String updatedAt;         // return as String (ISO-8601)
    private String usingPortal;       // enum as String
    private Long portalId;
}
