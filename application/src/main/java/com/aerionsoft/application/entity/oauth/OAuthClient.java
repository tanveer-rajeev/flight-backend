package com.aerionsoft.application.entity.oauth;

import com.aerionsoft.application.util.UserDateTimeUtil;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", unique = true, nullable = false)
    private String clientId;

    /**
     * Stored as BCrypt hash.
     */
    @Column(name = "client_secret_hash", nullable = false)
    private String clientSecretHash;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = UserDateTimeUtil.now();

    @Column(nullable = true)
    private String description;
}

