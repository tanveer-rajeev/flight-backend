package com.aerionsoft.application.entity.rolePermission;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permission_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}
