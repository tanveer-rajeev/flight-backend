package com.aerionsoft.application.service.oauth;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.client.auth.LoginResponse;
import com.aerionsoft.application.dto.oauth.LoginWithVlifeResponse;
import com.aerionsoft.application.dto.oauth.OauthMeResponse;
import com.aerionsoft.application.dto.oauth.VLifeAccessTokenResponse;
import com.aerionsoft.application.entity.LoginHistory;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.oauth.OauthPkceState;
import com.aerionsoft.application.entity.rolePermission.Role;
import com.aerionsoft.application.entity.rolePermission.RoleAssignment;
import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.enums.user.UserType;
import com.aerionsoft.application.filters.JwtUtil;
import com.aerionsoft.application.repository.user.LoginHistoryRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.oauth.OauthPkceStateRepository;
import com.aerionsoft.application.repository.access.RoleAssignmentRepository;
import com.aerionsoft.application.repository.access.RoleRepository;
import com.aerionsoft.application.service.audit.ActivityAuthAuditSupport;
import com.aerionsoft.application.service.user.ActiveUserPresenceService;
import jakarta.transaction.Transactional;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

@Service
public class VLifeOAuthService {

    private final OauthPkceStateRepository pkceStateRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RoleAssignmentRepository roleAssignmentRepo;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private LoginHistoryRepository loginHistoryRepo;

    @Autowired
    private ActiveUserPresenceService presenceService;

    @Autowired
    private ActivityAuthAuditSupport activityAuthAuditSupport;

    @Value("${vlife.client_id:#{null}}")
    private String clientId;

    @Value("${vlife.redirect_url:#{null}}")
    private String redirectUrl;

    @Value("${vlife.base_url:#{null}}")
    private String baseUrl;

    @Value("${vlife.jwks_url:#{null}}")
    private String jwksUrl;

    private final RestTemplate restTemplate;

    public VLifeOAuthService(RestTemplate restTemplate, OauthPkceStateRepository pkceStateRepository, UserRepository userRepository, RoleRepository roleRepository, RoleAssignmentRepository roleAssignmentRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.restTemplate = restTemplate;
        this.pkceStateRepository = pkceStateRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
    }

    public LoginWithVlifeResponse loginWithVlife() {
        // Generate PKCE
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        String state = UUID.randomUUID().toString();

        // Store PKCE to database
        OauthPkceState entity = OauthPkceState.builder()
                .state(state)
                .codeVerifier(codeVerifier)
                .createdAt(UserDateTimeUtil.now())
                .expiresAt(UserDateTimeUtil.now().plusMinutes(5))
                .used(false)
                .build();

        pkceStateRepository.save(entity);

        String url = baseUrl + "/oauth/auth";

        String oAuthProvider = UriComponentsBuilder
                .fromUriString(url)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUrl)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid profile email offline_access")
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .queryParam("state", state)
                .build()
                .toUriString();

        LoginWithVlifeResponse response = new LoginWithVlifeResponse();

        response.setUrl(oAuthProvider);

        return response;
    }

    @Transactional
    public LoginResponse getAccessToken(String code, String state) throws Exception {
        OauthPkceState oauthPkceState = pkceStateRepository.findByStateAndUsedFalse(state).orElseThrow(() -> ServiceExceptions.duplicate("Invalid or already used state"));

        if (oauthPkceState.getExpiresAt().isBefore(UserDateTimeUtil.now())) {
            throw ServiceExceptions.microservice("OAuth state has expired");
        }

        String codeVerifier = oauthPkceState.getCodeVerifier();

        VLifeAccessTokenResponse vLifeAccessTokenResponse = getVlifeAccessToken(code, codeVerifier);

        // Oauth me
        OauthMeResponse oauthMe = getUserInfoFromVlife(vLifeAccessTokenResponse.getAccessToken());

        String email = oauthMe.getEmail();
        String firstName = oauthMe.getFirstName();
        String lastName = oauthMe.getLastName();
        String currencyCode = oauthMe.getCurrencyCode() == null || oauthMe.getCurrencyCode().equals("") ? oauthMe.getCurrencyCode(): "USD";

        Optional<User> user = userRepository.findByEmail(email);

        if (user.isEmpty()) {
            // Create new user as type vlife
            String random10 = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            String fullName = firstName + " " + lastName;

            Set<String> defaultRoles = new HashSet<>();
            defaultRoles.add("USER");

            User newUser =  User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(random10))
                    .isVerified(true)
                    .fullName(fullName)
                    .isActive(true)
                    .createdAt(UserDateTimeUtil.now())
                    .isAgency(false)
                    .currency(Currency.fromCode(currencyCode))
                    .balance(0.0)
                    .roles(defaultRoles)
                    .userType(UserType.VLIFE)
                    .build();

            userRepository.save(newUser);

            Role vLifeRole = roleRepository.findBySlug("vlife").orElseThrow(()-> new ResourceNotFoundException("Role"));


            RoleAssignment roleAssignment = new RoleAssignment();
            roleAssignment.setRole(vLifeRole);
            roleAssignment.setEntityId(newUser.getId());
            roleAssignment.setEntityType("USER");

            roleAssignmentRepository.save(roleAssignment);

            Set<Role> userRoles = roleAssignmentRepo.findRolesByEntity("USER", newUser.getId());

            Set<SimpleGrantedAuthority> authorities = userRoles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getSlug()))
                    .collect(java.util.stream.Collectors.toSet());

            authorities.add(new SimpleGrantedAuthority("ROLE_user"));


            UserDetails userDetails = new org.springframework.security.core.userdetails.User(newUser.getEmail(),
                    newUser.getPassword(), authorities);

            String token = jwtUtil.generateToken(userDetails, "user", false);

            presenceService.markOnline("user", newUser.getId(), null, null);

            return new LoginResponse(token);
        }

        Set<Role> userRoles = roleAssignmentRepo.findRolesByEntity( "USER", user.get().getId());

        Set<SimpleGrantedAuthority> authorities = userRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getSlug()))
                .collect(java.util.stream.Collectors.toSet());

        if (userRoles.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_user"));
        }

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(user.get().getEmail(),
                user.get().getPassword(), authorities);

        String token = jwtUtil.generateToken(userDetails, "user", false);

        loginHistoryRepo.save(LoginHistory.builder()
                .user(user.get())
                .loginAt(UserDateTimeUtil.now())
                .build());

        activityAuthAuditSupport.logUserLogin(user.get(), null, null);

        presenceService.markOnline("user", user.get().getId(), null, null);

        return new LoginResponse(token);
    }

    public VLifeAccessTokenResponse getVlifeAccessToken(String code, String codeVerifier) {
        try {
            if (clientId == null || clientId.isBlank()) throw ServiceExceptions.microservice("clientId is not set");
            if (redirectUrl == null || redirectUrl.isBlank()) throw ServiceExceptions.microservice("redirectUrl is not set");
            if (baseUrl == null || baseUrl.isBlank()) throw ServiceExceptions.microservice("baseUrl is not set");
            if (jwksUrl == null || jwksUrl.isBlank()) throw ServiceExceptions.microservice("JWKS URL is not set");

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "authorization_code");
            formData.add("code", code);
            formData.add("redirect_uri", redirectUrl);
            formData.add("client_id", clientId);
            formData.add("code_verifier", codeVerifier);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formData, headers);
            String url = baseUrl+"/oauth/token";

            ResponseEntity<VLifeAccessTokenResponse> response = restTemplate.postForEntity(url, entity, VLifeAccessTokenResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw ServiceExceptions.microservice("VLife API error: " + response.getStatusCode());
            }

            VLifeAccessTokenResponse vLifeAccessTokenResponse = response.getBody();

            JwtClaims claims = verifyJwtWithJWKS(vLifeAccessTokenResponse.getIdToken());

            System.out.println("JWT claims: " + claims);

            return vLifeAccessTokenResponse;

        } catch (Exception e) {
            throw ServiceExceptions.microservice("Unexpected error occurred while calling VLife API: ", e);
        }
    }

    private JwtClaims verifyJwtWithJWKS(String accessToken) throws Exception {

        HttpsJwks httpsJwks = new HttpsJwks(baseUrl + jwksUrl);
        HttpsJwksVerificationKeyResolver keyResolver =
                new HttpsJwksVerificationKeyResolver(httpsJwks);

        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(30)
                .setVerificationKeyResolver(keyResolver)
                .setExpectedIssuer(baseUrl+"/")
                .setExpectedAudience(clientId)
                .build();

        return jwtConsumer.processToClaims(accessToken);
    }

    private OauthMeResponse getUserInfoFromVlife(String accessToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = baseUrl+"/oauth/me";

        ResponseEntity<OauthMeResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                OauthMeResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new Exception("VLife API error: " + response.getStatusCode());
        }

        return response.getBody();
    }

    private String generateCodeVerifier() {
       SecureRandom SECURE_RANDOM = new SecureRandom();

        byte[] randomBytes = new byte[50];
        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = sha256.digest(
                    codeVerifier.getBytes(StandardCharsets.US_ASCII));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hashedBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }
}
