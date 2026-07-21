package com.aerionsoft.application.entity.cms;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "content_section_map")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class ContentSectionMap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}


