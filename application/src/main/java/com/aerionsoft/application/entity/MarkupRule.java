package com.aerionsoft.application.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.aerionsoft.application.dto.flight.MarkupCombinedCondition;
import com.aerionsoft.application.enums.flight.MarkupFilterMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "markup_rules")
public class MarkupRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "markup_plan_id")
    @JsonBackReference
    private MarkupPlan markupPlan;

    // Expose markupPlanId in JSON response
    @JsonProperty("markupPlanId")
    public Long getMarkupPlanId() {
        return markupPlan != null ? markupPlan.getId() : null;
    }

    private String provider; // SABRE, etc.
    private String origin; // ANY, BD, NBD
    private Integer priority;
    private String airlineCode;
    private String airlineName;
    private Boolean isSuspended;
    private String flyType; // ANY, INT, LOCAL
    private String appliedOn; // ALWAYS, SPECIFIC_PERIOD
    private LocalDate startDate;
    private LocalDate endDate;
    private String routes; // Comma separated
    @Column(name = "booking_codes")
    @JsonAlias("cabinClasses")
    private String bookingCodes; // Comma separated fare class codes, e.g. Y,M,B

    @Enumerated(EnumType.STRING)
    @Column(name = "filter_mode", nullable = false)
    private MarkupFilterMode filterMode = MarkupFilterMode.INDIVIDUAL;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "combined_conditions", columnDefinition = "jsonb")
    private List<MarkupCombinedCondition> combinedConditions;

    private Double minBaseFare;
    private Double maxBaseFare;
    private Boolean isActive;
    private Integer commission;

    // Commission
    private Double commissionProvision;
    private Double commissionLessApplied;
    private String commissionType; // PERCENTAGE, FIXED

    // Markup
    private Double markupValue;
    private String markupType; // PERCENTAGE, FIXED
}

