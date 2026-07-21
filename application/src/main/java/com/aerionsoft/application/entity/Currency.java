package com.aerionsoft.application.entity;

import jakarta.persistence.*;
import lombok.*;

import com.aerionsoft.application.entity.listener.UserTimestampListener;
import com.aerionsoft.application.util.UserDateTimeUtil;

import java.time.LocalDateTime;

@Entity
@Table(name = "currencies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(UserTimestampListener.class)
public class Currency implements HasCreatedUserTimestamp, HasUpdatedUserTimestamp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = false, nullable = false)
    private String code;

    @Column(nullable = false)
    private Double rate;

    @Column(name = "reverse_rate", nullable = false)
    private Double reverseRate;

    private String providerId;

    @Column(name = "channel")
    private String channel;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = UserDateTimeUtil.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = UserDateTimeUtil.now();
        }
        if (this.createdTimeOffset == null) {
            this.createdTimeOffset = UserDateTimeUtil.currentOffset();
        }
        if (this.updatedTimeOffset == null) {
            this.updatedTimeOffset = UserDateTimeUtil.currentOffset();
        }
        this.reverseRate = 1.0 / this.rate;
    }
}