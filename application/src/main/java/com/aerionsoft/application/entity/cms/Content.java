package com.aerionsoft.application.entity.cms;

import com.aerionsoft.application.enums.cms.ContentStatus;
import com.aerionsoft.application.enums.cms.ContentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
    @Table(name = "content")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType type;

    @Column(nullable = false)
    private String title;

    @Column(name = "slug", unique = true, nullable = false)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "meta_title")
    private String metaTitle;

    @Column(name = "meta_description", columnDefinition = "TEXT")
    private String metaDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentStatus status = ContentStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    // One-to-many relationship with sections
    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ContentSectionMap> contentSections = new ArrayList<>();

    // One-to-one relationship with category
    @OneToOne(mappedBy = "content", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private ContentCategoryMap contentCategory;

    // One-to-one relationship with tag
    @OneToOne(mappedBy = "content", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private ContentTagMap contentTag;
}


