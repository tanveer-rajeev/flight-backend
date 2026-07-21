package com.aerionsoft.application.dto.business;

import lombok.Data;

import java.util.Date;

@Data
public class BusinessRequest {
    private String companyName;
    private String companyEmail;
    private String companyAddress;
    private String companyPhone;
    private Long motherUserId;
    private String companyLogo;
    private String companyLicence;
    private String civilAviationCertNumber;
    private Date civilAviationCertExpiryDate;
    private String addressProof;
    private String attachment;
    private String representativeName;
    private String representativeMobile;
    private String representativeEmail;
    private String representativePosition;
    private String digitalSignature;
}
