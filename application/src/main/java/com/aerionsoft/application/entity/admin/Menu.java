package com.aerionsoft.application.entity.admin;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "menus")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Menu parentId;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 100, nullable = false, unique = true)
    private String slug;

    @Column(length = 100)
    private String icon;

    @Column(nullable = false)
    private Integer step = 0;

    @Column(name = "is_active")
    private Boolean isActive;
}
