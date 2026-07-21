package com.aerionsoft.application.entity.admin;

import com.aerionsoft.application.entity.rolePermission.Permission;
import com.aerionsoft.application.entity.rolePermission.Role;
import jakarta.persistence.*;
import lombok.Setter;

@Entity
@Table(name = "menus_permissions")
@Setter
public class MenuPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

}
