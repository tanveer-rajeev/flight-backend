package com.aerionsoft.application.entity;

import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.enums.business.BusinessStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "businesses")
@EntityListeners(AuditingEntityListener.class)
public class BusinessEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "company_email")
    private String companyEmail;

    @Column(name = "company_address")
    private String companyAddress;

    @Column(name = "company_phone")
    private String companyPhone;

    @Column(name = "company_logo")
    private String companyLogo;

    @Column(name = "company_licence")
    private String companyLicence;

    @Column(name = "civil_aviation_cert_number")
    private String civilAviationCertNumber;

    @Column(name = "civil_aviation_cert_expiry_date")
    @Temporal(TemporalType.DATE)
    private Date civilAviationCertExpiryDate;

    @Column(name = "address_proof")
    private String addressProof;

    @Column(name = "attachment")
    private String attachment;

    @Column(name = "representative_name")
    private String representativeName;

    @Column(name = "representative_mobile")
    private String representativeMobile;

    @Column(name = "representative_email")
    private String representativeEmail;

    @Column(name = "representative_position")
    private String representativePosition;

    @Column(name = "balance")
    private BigDecimal balance;


    @Column(name = "credit_limit", precision = 19, scale = 4)
    private BigDecimal creditLimit;


    @Column(name = "digital_signature")
    private String digitalSignature;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    @ManyToOne
    @JoinColumn(name = "mother_user_id")
    private User motherUser;

    @Enumerated(EnumType.STRING)
    private BusinessStatus status;
}