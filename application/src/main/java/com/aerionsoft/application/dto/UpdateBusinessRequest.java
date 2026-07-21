package com.aerionsoft.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class UpdateBusinessRequest {
    private String companyName;
    private String companyEmail;
    private String companyAddress;
    private String companyPhone;
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
    private BigDecimal creditLimit;
}

