package com.aerionsoft.application.service.gateway;

import com.aerionsoft.application.dto.gateway.NGeniusCredentialDto;
import com.aerionsoft.application.entity.paymentGateway.NGeniusCredential;
import com.aerionsoft.application.repository.gateway.NGeniusCredentialRepository;
import org.springframework.stereotype.Service;

@Service
public class NGeniusCredentialService {
    private final NGeniusCredentialRepository nGeniusCredentialRepository;

    public NGeniusCredentialService(NGeniusCredentialRepository nGeniusCredentialRepository) {
        this.nGeniusCredentialRepository = nGeniusCredentialRepository;
    }

    /**
     * Get NGeniusCredential Service
     *
     * @return NGeniusCredentialDto
     */
    public NGeniusCredentialDto getCredential() {
        NGeniusCredential credential = nGeniusCredentialRepository.findById(1L).orElse(null);
        if (credential == null) {
            return null;
        }
        return mapToDto(credential);
    }

    /**
     * NGeniusCredentialDto
     *
     * @param credential NGeniusCredential
     * @return NGeniusCredentialDto
     */
    private NGeniusCredentialDto mapToDto(NGeniusCredential credential) {
        NGeniusCredentialDto dto = new NGeniusCredentialDto();
        dto.setId(credential.getId());
        dto.setBaseUrl(credential.getBaseUrl());
        dto.setOutletReference(credential.getOutletReference());
        dto.setApiKey(credential.getApiKey());
        dto.setCancelUrl(credential.getCancelUrl());
        dto.setRedirectUrl(credential.getRedirectUrl());
        dto.setIsActive(credential.isActive());
        return  dto;
    }
}
