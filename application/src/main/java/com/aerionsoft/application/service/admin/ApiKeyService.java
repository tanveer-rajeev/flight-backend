package com.aerionsoft.application.service.admin;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.entity.ApiKey;
import com.aerionsoft.application.repository.common.ApiKeyRepository;
import com.aerionsoft.application.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final String superKeyPlain;

    // Inject encrypted key from application.properties or application.yml
    public ApiKeyService(ApiKeyRepository apiKeyRepository,
                         @Value("${security.super-encrypted-key}") String superEncryptedKey) {
        this.apiKeyRepository = apiKeyRepository;
        this.superKeyPlain = EncryptionUtil.decrypt(superEncryptedKey);
    }

    public ApiKey generateKey(String description) {
        String generatedKey = UUID.randomUUID().toString();
        ApiKey apiKey = new ApiKey();
        apiKey.setKey(generatedKey);
        apiKey.setDescription(description);
        apiKey.setCreatedAt(UserDateTimeUtil.now());
        return apiKeyRepository.save(apiKey);
    }

    public boolean isValidKey(String key) {
        return apiKeyRepository.findByKey(key).isPresent();
    }

    public boolean isValidSuperKey(String key) {
        return key != null && key.equals(superKeyPlain) || apiKeyRepository.findByKey(key).isPresent();
    }
}
