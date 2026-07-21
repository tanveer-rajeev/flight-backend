package com.aerionsoft.application.entity.visa;

import com.aerionsoft.application.util.UserDateTimeUtil;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "visa_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisaInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_id", nullable = true)
    private Long formId;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(name = "visa_type", nullable = false, length = 100)
    private String visaType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "required_documents", columnDefinition = "TEXT[]")
    private String[] requiredDocuments;

    @Column(columnDefinition = "TEXT")
    private String rules;

    @Column(name = "processing_time", length = 255)
    private String processingTime;

    @Column(name = "fee_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal feeAmount;

    @Column(length = 10)
    private String currency = "USD";

    @Column(name = "created_at")
    private LocalDateTime createdAt = UserDateTimeUtil.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = UserDateTimeUtil.now();

    @Column(name= "banner", nullable = true)
    private String banner;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = UserDateTimeUtil.now();
    }
}
