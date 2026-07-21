package com.aerionsoft.application.entity.client;

import com.aerionsoft.application.util.UserDateTimeUtil;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private User agencyUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(unique = true, length = 100, nullable = false)
    private String email;

    @Column(nullable = false)
    private String address;

    @Column(name = "phone_number", length = 15, nullable = false)
    private String phoneNumber;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "update_at")
    private LocalDateTime updateAt;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "paid_amount", precision = 19, scale = 4)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "payable_amount",  precision = 19, scale = 4)
    private BigDecimal payableAmount  = BigDecimal.ZERO;

    @Column(name = "initial_balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal initialBalance = BigDecimal.ZERO;

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SupplierProviderMapping> providerMappings = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        this.createAt = UserDateTimeUtil.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updateAt = UserDateTimeUtil.now();
    }
}
