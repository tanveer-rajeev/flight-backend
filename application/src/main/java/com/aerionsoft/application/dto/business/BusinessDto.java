package com.aerionsoft.application.dto.business;

import com.aerionsoft.application.dto.salesperson.SalesPersonResponseDto;
import com.aerionsoft.application.enums.business.BusinessStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class BusinessDto {
    private Long id;
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
    private BigDecimal balance;
    private BigDecimal creditLimit;
    private String digitalSignature;
    private Long motherUserId;
    private String motherUserFullName;
    private String role;
    private BusinessStatus status;
    private Double motherCurrentBalance;
    private Double motherTotalCredit;
    private Double motherTotalDebit;
    private Double motherTotalCreditInUC;
    private Double motherTotalDebitInUC;
    private String motherUserCurrency;
    private Double motherCurrentBalanceInUC;

    private String AgencyCode;

    private List<SalesPersonResponseDto> salesPersons;
}

