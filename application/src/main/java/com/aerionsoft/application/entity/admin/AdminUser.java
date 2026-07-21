package com.aerionsoft.application.entity.admin;

import com.aerionsoft.application.util.UserDateTimeUtil;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "admin_users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Boolean isVerified = false; // Use Boolean, since default is important

    @Column(nullable = false)
    private LocalDateTime createdAt = UserDateTimeUtil.now();


    @Column(nullable = true)
    private String fullName;

    @Column(nullable = true)
    private String phoneNumber;

    @Column(nullable = true)
    private String image;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = true)
    private String address;

    @Column(nullable = true)
    private String currency;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "admin_user_roles", joinColumns = @JoinColumn(name = "admin_user_id"))
    @Column(name = "role")
    private Set<String> roles;

}
