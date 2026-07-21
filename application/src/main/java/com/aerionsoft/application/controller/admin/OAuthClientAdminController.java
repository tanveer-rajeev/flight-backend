package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.oauth.CreateOAuthClientRequest;
import com.aerionsoft.application.dto.oauth.CreateOAuthClientResponse;
import com.aerionsoft.application.entity.oauth.OAuthClient;
import com.aerionsoft.application.repository.oauth.OAuthClientRepository;
import com.aerionsoft.application.service.oauth.OAuthClientService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/admin/oauth-clients")
@PreAuthorize("hasRole('admin')")
public class OAuthClientAdminController {

    private final OAuthClientService oAuthClientService;
    private final OAuthClientRepository oAuthClientRepository;

    public OAuthClientAdminController(OAuthClientService oAuthClientService, OAuthClientRepository oAuthClientRepository) {
        this.oAuthClientService = oAuthClientService;
        this.oAuthClientRepository = oAuthClientRepository;
    }

    @PostMapping
    public ResponseEntity<BaseResponse<CreateOAuthClientResponse>> create(@Valid @RequestBody CreateOAuthClientRequest request) {
        if (oAuthClientRepository.findByClientId(request.clientId()).isPresent()) {
            return ResponseEntity.badRequest().body(BaseResponse.error(400, "client_id_already_exists", null));
        }

        OAuthClient client = oAuthClientService.createClient(request.clientId(), request.clientSecret(), request.description());

        // Return the raw secret once (caller must store it); DB stores only BCrypt hash.
        return ResponseEntity.ok(BaseResponse.ok("OAuth client created", new CreateOAuthClientResponse(
                client.getClientId(),
                request.clientSecret(),
                client.getDescription(),
                Boolean.TRUE.equals(client.getActive())
        )));
    }

    @PostMapping("/generate")
    public ResponseEntity<BaseResponse<CreateOAuthClientResponse>> generate(@RequestParam(required = false) String clientId,
                                                                           @RequestParam(required = false) String description) {
        String generatedClientId = (clientId == null || clientId.isBlank()) ? "client_" + UUID.randomUUID() : clientId;
        String generatedSecret = "secret_" + UUID.randomUUID();

        if (oAuthClientRepository.findByClientId(generatedClientId).isPresent()) {
            return ResponseEntity.badRequest().body(BaseResponse.error(400, "client_id_already_exists", generatedClientId));
        }

        OAuthClient client = oAuthClientService.createClient(generatedClientId, generatedSecret, description);

        return ResponseEntity.ok(BaseResponse.ok("OAuth client generated", new CreateOAuthClientResponse(
                client.getClientId(),
                generatedSecret,
                client.getDescription(),
                Boolean.TRUE.equals(client.getActive())
        )));
    }
}

