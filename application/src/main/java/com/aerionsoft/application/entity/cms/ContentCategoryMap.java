package com.aerionsoft.application.entity.cms;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "content_category_map")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class ContentCategoryMap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false, unique = true)
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}


