package com.aerionsoft.application.service.oauth;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.entity.oauth.OAuthClient;
import com.aerionsoft.application.repository.oauth.OAuthClientRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class OAuthClientService {

    private final OAuthClientRepository oauthClientRepository;
    private final PasswordEncoder passwordEncoder;

    public OAuthClientService(OAuthClientRepository oauthClientRepository, @Lazy PasswordEncoder passwordEncoder) {
        this.oauthClientRepository = oauthClientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Validates Basic-Auth credentials against DB.
     */
    public boolean isValidClient(String clientId, String clientSecret) {
        if (clientId == null || clientId.isBlank() || clientSecret == null) return false;

        return oauthClientRepository.findByClientId(clientId)
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .map(c -> passwordEncoder.matches(clientSecret, c.getClientSecretHash()))
                .orElse(false);
    }

    /**
     * Utility for seeding/creating clients (hashes the secret).
     */
    public OAuthClient createClient(String clientId, String rawClientSecret, String description) {
        OAuthClient client = OAuthClient.builder()
                .clientId(clientId)
                .clientSecretHash(passwordEncoder.encode(rawClientSecret))
                .description(description)
                .active(true)
                .createdAt(UserDateTimeUtil.now())
                .build();
        return oauthClientRepository.save(client);
    }
}
