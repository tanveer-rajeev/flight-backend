package com.aerionsoft.application.entity;

import com.aerionsoft.application.util.UserDateTimeUtil;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "breaking_news")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BreakingNews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Headline shown in the ticker / banner */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /** Full body text (optional, for a detail view) */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** Who should see this news: ALL, USER, AGENCY */
    @Column(name = "target", nullable = false, length = 20)
    private String target;

    /** Ordering weight — higher number = shown first */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    /** Whether this news is currently published */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Optional: news will not be shown before this date */
    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    /** Optional: news expires after this date */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** Link users can follow for more details */
    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    private String imageUrl;


    @PrePersist
    protected void onCreate() {
        createdAt = UserDateTimeUtil.now();
        updatedAt = UserDateTimeUtil.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = UserDateTimeUtil.now();
    }
}

