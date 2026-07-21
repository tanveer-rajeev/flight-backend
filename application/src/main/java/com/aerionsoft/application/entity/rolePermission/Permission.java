package com.aerionsoft.application.entity.rolePermission;

import com.aerionsoft.application.enums.access.PermissionModule;
import com.aerionsoft.application.enums.access.PermissionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true, nullable = false, length = 100)
    private String slug;

    @Enumerated(EnumType.STRING)
    private PermissionType type;

    @Enumerated(EnumType.STRING)
    private PermissionModule module;

    @Column(nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "permission_group_id")
    private PermissionGroup permissionGroup;
}
