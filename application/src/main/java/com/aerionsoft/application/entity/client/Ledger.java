package com.aerionsoft.application.entity.client;

import jakarta.persistence.*;
import lombok.*;

import com.aerionsoft.application.entity.HasCreatedUserTimestamp;
import com.aerionsoft.application.entity.HasUpdatedUserTimestamp;
import com.aerionsoft.application.entity.listener.UserTimestampListener;
import com.aerionsoft.application.util.UserDateTimeUtil;

import java.time.LocalDateTime;

@Entity
@Table(name = "ledgers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(UserTimestampListener.class)
public class Ledger implements HasCreatedUserTimestamp, HasUpdatedUserTimestamp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agency_id")
    private Long agencyId;

    @Column(name = "title")
    private String title;

    @Column(name = "image")
    private String image;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    @PrePersist
    protected void onCreate() {
        this.createdAt = UserDateTimeUtil.now();
        this.createdTimeOffset = UserDateTimeUtil.currentOffset();
        this.updatedAt = UserDateTimeUtil.now();
        this.updatedTimeOffset = UserDateTimeUtil.currentOffset();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = UserDateTimeUtil.now();
        this.updatedTimeOffset = UserDateTimeUtil.currentOffset();
    }
}
