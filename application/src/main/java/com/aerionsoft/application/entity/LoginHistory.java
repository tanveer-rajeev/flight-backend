package com.aerionsoft.application.entity;

import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.entity.client.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id") // references 'users' table
    private User user;

    @ManyToOne
    @JoinColumn(name = "admin_user_id", referencedColumnName = "id") // references 'admin_users' table
    private AdminUser adminUser;


    private LocalDateTime loginAt;
    private String ipAddress;
    private String userAgent;
}