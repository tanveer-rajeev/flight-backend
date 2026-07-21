package com.aerionsoft.application.entity;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.enums.finance.AccountHeadType;
import com.aerionsoft.application.enums.common.UsingPortal;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_head")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountHead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_head_title", nullable = false)
    private String accountHeadTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountHeadType type;

    @Column(name = "parent_id")
    private Long parentId = 0L;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    @Enumerated(EnumType.STRING)
    @Column(name = "using_portal", nullable = false)
    private UsingPortal usingPortal;

    @Column(name = "portal_id")
    private Long portalId;

    @PrePersist
    protected void onCreate() {
        createdAt = UserDateTimeUtil.now();
        updatedAt = UserDateTimeUtil.now();
        if (parentId == null) {
            parentId = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = UserDateTimeUtil.now();
    }
}

