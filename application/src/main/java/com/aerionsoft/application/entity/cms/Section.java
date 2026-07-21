package com.aerionsoft.application.entity.cms;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sections")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "image_url")
    private String imageUrl;
}


