package com.aerionsoft.application.gateway.providers;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.gateway.CardDto;
import com.aerionsoft.application.dto.gateway.NGeniusCredentialDto;
import com.aerionsoft.application.dto.gateway.NGeniusOneStagePaymentResponseDto;
import com.aerionsoft.application.dto.payment.PaymentRequestDto;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.paymentGateway.Payment;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.enums.common.NGeniusActionType;
import com.aerionsoft.application.enums.wallet.TransactionSourceType;
import com.aerionsoft.application.gateway.contracts.GatewayClientInterface;
import com.aerionsoft.application.gateway.dtos.ngenius.*;
import com.aerionsoft.application.repository.payment.PaymentRepository;
import com.aerionsoft.application.repository.wallet.TransactionRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.service.gateway.NGeniusCredentialService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class NGenius implements GatewayClientInterface {
    private static final long EXPIRY_SAFETY_MARGIN_SECONDS = 30L;
    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private String key;
    private String baseUrl;
    private String redirectUrl;
    private String cancelUrl;
    private String outletReference;
    private HttpClient httpClient;
    private ObjectMapper mapper;
    private volatile NGeniusAccessTokenDto cachedToken;
    private volatile Instant tokenExpiry;

    public NGenius(NGeniusCredentialService nGeniusCredentialService, PaymentRepository paymentRepository, TransactionRepository transactionRepository, UserRepository userRepository) {
        setCredential(nGeniusCredentialService);
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Set credential for NGENIUS API
     *
     * @param nGeniusCredentialService NGeniusCredentialService
     */
    private void setCredential(NGeniusCredentialService nGeniusCredentialService) {
        NGeniusCredentialDto credential = nGeniusCredentialService.getCredential();
        if (credential == null) {
            // No credential configured, skip initialization
            return;
        }
        this.baseUrl = credential.getBaseUrl();
        this.redirectUrl = credential.getRedirectUrl();
        this.cancelUrl = credential.getCancelUrl();
        this.outletReference = credential.getOutletReference();
        this.key = "Basic " + credential.getApiKey();
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    @Override
    public String getGatewayId() {
        return "NGENIUS";
    }

    /**
     * NGenius Process payment
     *
     * @param request PaymentRequestDto
     * @param payment Payment
     * @return Object
     */
    @Override
    public Object processPayment(PaymentRequestDto request, Payment payment) {
        NGeniusCreateOrderDto nGeniusCreateOrderDto = new NGeniusCreateOrderDto();
        int ceilAmount = (int) Math.ceil(request.getAmount());
        nGeniusCreateOrderDto.getAmount().setValue(ceilAmount);
        nGeniusCreateOrderDto.getAmount().setCurrencyCode(request.getCurrency());
        nGeniusCreateOrderDto.getMerchantAttributes().setRedirectUrl(this.redirectUrl);
        nGeniusCreateOrderDto.getMerchantAttributes().setCancelUrl(this.cancelUrl);

        NGeniusCreateOrderResponse response = createOrder(nGeniusCreateOrderDto);

        String ts = response.getEmbedded().getPayment().get(0).getUpdateDateTime();
        OffsetDateTime odt = OffsetDateTime.parse(ts);

        // Update payment
        payment.setCurrency(response.getAmount().getCurrencyCode());
        payment.setAmount(response.getAmount().getValue());
        payment.setNgeniusOrderReference(response.getReference());
        payment.setNgeniusPaymentReference(response.getEmbedded().getPayment().get(0).getReference());
        payment.setNgeniusPaymentState(response.getEmbedded().getPayment().get(0).getState());
        payment.setNgeniusUpdateDateTime(odt.toLocalDateTime());
        payment.setNgeniusOutletId(response.getOutletId());

        paymentRepository.save(payment);

        return Map.of(
                "payment_id", payment.getId(),
                "hostedUrl", response.getLinks().getPayment().getHref(),
                "state", response.getEmbedded().getPayment().get(0).getState(),
                "orderReference", response.getReference(),
                "status", "Order is created, Customer need to pay"
        );
    }

    /**
     * Get order status
     *
     * @param orderReference orderReference
     * @return Object
     */
    public Object getOrderStatus(String orderReference) {
        return retrieveOrderStatus(orderReference);
    }

    /**
     * Create NGenius Hosted Payment Order
     *
     * @param nGeniusCreateOrderDto NGeniusCreateOrderDto
     * @return NGeniusCreateOrderResponse
     */
    private NGeniusCreateOrderResponse createOrder(NGeniusCreateOrderDto nGeniusCreateOrderDto) {
        NGeniusAccessTokenDto token = getAccessToken();
        String bearerToken = "Bearer " + token.getAccessToken();

        try {
            String json = mapper.writeValueAsString(nGeniusCreateOrderDto);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/transactions/outlets/" + outletReference + "/orders"))
                    .header("Authorization", bearerToken)
                    .header("Accept", "application/vnd.ni-payment.v2+json")
                    .header("Content-Type", "application/vnd.ni-payment.v2+json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) {
                throw ServiceExceptions.payment("NGENIUS: Order failed: " + response.statusCode() + " body: " + response.body());
            }

            return mapper.readValue(response.body(), NGeniusCreateOrderResponse.class);
        } catch (Exception exception) {
            Thread.currentThread().interrupt(); // restore interrupt if interrupted
            throw ServiceExceptions.payment("Error fetching: " + exception.getMessage());
        }
    }

    /**
     * Check NGenius Order Status
     *
     * @param orderReference orderReference
     * @return Object
     */
    private Object retrieveOrderStatus(String orderReference) {
        NGeniusAccessTokenDto token = getAccessToken();
        String bearerToken = "Bearer " + token.getAccessToken();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/transactions/outlets/" + outletReference + "/orders/" + orderReference))
                    .header("Authorization", bearerToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw ServiceExceptions.payment("NGENIUS: Order retrieve failed: " + response.statusCode() + " body: " + response.body());
            }

            Payment payment = paymentRepository.findByNgeniusOrderReference(orderReference).orElseThrow(() -> new ResourceNotFoundException("NGENIUS: Order reference", orderReference));

            if (payment.getStatus().equals("succeeded") || payment.getStatus().equals("failed")) {
                return Map.of("status", payment.getStatus(), "orderReference", orderReference);
            }

           else {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode root = objectMapper.readTree(response.body());

                String status = root.path("_embedded").path("payment").get(0).get("state").asText();

                payment.setNgeniusPaymentState(status);

                payment.setStatus(
                        status.equals("PURCHASED") || status.equals("SUCCEEDED")
                                ? "succeeded"
                                : "failed"
                );

                paymentRepository.save(payment);

                if (status.equals("PURCHASED") || status.equals("SUCCEEDED")) {
                    // Find transaction
                    Transaction transaction = transactionRepository.findFirstBySourceTypeAndSourceId(
                            TransactionSourceType.PAYMENT.name(), payment.getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Transaction"));

                    // Find User
                    User user = userRepository.findById((Long)transaction.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User"));
                    // Update user balance
                    double balance = user.getBalance() == null ? transaction.getAmount() : user.getBalance() + transaction.getAmount();
                    user.setBalance(balance);
                    userRepository.save(user);
                }

                return Map.of("status", status, "orderReference", orderReference);
            }
        } catch (Exception exception) {
            Thread.currentThread().interrupt();
            throw ServiceExceptions.payment("NGENIUS: Error fetching: " + exception.getMessage());
        }
    }

    /**
     * Get Access token
     *
     * @return AccessTokenDto
     */
    private NGeniusAccessTokenDto getAccessToken() {
        NGeniusAccessTokenDto token = cachedToken;
        Instant expiry = tokenExpiry;

        // Checking cache token validity
        if (token != null && expiry != null) {
            Instant now = Instant.now();
            if (now.isBefore(expiry.minusSeconds(EXPIRY_SAFETY_MARGIN_SECONDS))) {
                return token;
            }
        }

        // Fetch new token from provider
        NGeniusAccessTokenDto fresh = getAccessTokenFromProvider();
        Instant newExpiry = Instant.now().plusSeconds(Math.max(0, fresh.getExpiresIn()));
        this.cachedToken = fresh;
        this.tokenExpiry = newExpiry;
        return fresh;
    }

    /**
     * Get Access Token fron N-Genius
     *
     * @return AccessTokenDto
     */
    private NGeniusAccessTokenDto getAccessTokenFromProvider() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/identity/auth/access-token"))
                    .header("Authorization", key)
                    .header("Content-Type", "application/vnd.ni-identity.v1+json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw ServiceExceptions.payment("Failed to get N-Genius access token : HTTP error code : " + response.statusCode());
            }

            NGeniusAccessTokenDto accessTokenDto = mapper.readValue(response.body(), NGeniusAccessTokenDto.class);

            if (accessTokenDto == null || accessTokenDto.getAccessToken() == null) {
                throw ServiceExceptions.payment("Invalid token response from N-Genius: " + response.body());
            }

            return accessTokenDto;

        } catch (Exception exception) {
            Thread.currentThread().interrupt(); // restore interrupt if interrupted
            throw ServiceExceptions.payment("Error fetching N-Genius access token", exception);
        }
    }

    /**
     * Ngenius one stage payment
     *
     * @param nGeniusOneStageCreateOrderDto NGeniusCreateOrderDto
     * @param payment Payment
     * @return Object as response
     */
    private Object oneStagePayment(NGeniusOneStageCreateOrderDto nGeniusOneStageCreateOrderDto, Payment payment) {
        try {
            NGeniusAccessTokenDto token = getAccessToken();
            String bearerToken = "Bearer " + token.getAccessToken();

            String json = mapper.writeValueAsString(nGeniusOneStageCreateOrderDto);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/transactions/outlets/" + outletReference + "/payment/card"))
                    .header("Authorization", bearerToken)
                    .header("Accept", "application/vnd.ni-payment.v2+json")
                    .header("Content-Type", "application/vnd.ni-payment.v2+json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) {
                throw ServiceExceptions.payment("Payment processing failed. Please try again.");
            }

            NGeniusOneStagePaymentResponseDto dto = mapper.readValue(response.body(), NGeniusOneStagePaymentResponseDto.class);

            payment.setNgeniusPaymentReference(dto.getReference());
            payment.setNgeniusPaymentState(dto.getState());
            payment.setNgeniusOrderReference(dto.getOrderReference());

            if (dto.getPaymentMethod() != null) {
                payment.setCardExpiryMonth(dto.getPaymentMethod().getExpiry());
                payment.setCardBrand(dto.getPaymentMethod().getName());
                payment.setCardHolderName(dto.getPaymentMethod().getCardholderName());
                payment.setMaskedPan(dto.getPaymentMethod().getPan());
            }
            payment.setNgeniusOutletId(dto.getOutletId());
            payment.setOriginIp(dto.getOriginIp());

            paymentRepository.save(payment);

            if ("AWAIT_3DS".equalsIgnoreCase(dto.getState())) {
                NGeniusThreeDSResponseDto nGeniusThreeDSResponseDTO = sendThreeDSChallenge(new NGeniusThreeDSRequestDto(), payment.getNgeniusOrderReference(), payment.getNgeniusPaymentReference());

                payment.setAcsTransId(nGeniusThreeDSResponseDTO.getThreeDS2().getAcsTransID());
                payment.setGatewayMid(nGeniusThreeDSResponseDTO.getMid());

                Map<String, Object> threeDs = new HashMap<>();

                threeDs.put("acsURL", nGeniusThreeDSResponseDTO.getThreeDS2().getAcsURL());
                threeDs.put("base64EncodedCReq", nGeniusThreeDSResponseDTO.getThreeDS2().getBase64EncodedCReq());
                threeDs.put("threeDSServerTransID", nGeniusThreeDSResponseDTO.getThreeDS2().getThreeDSServerTransID());

                return Map.of(
                        "status", dto.getState(),
                        "orderReference", dto.getOrderReference(),
                        "paymentReference", dto.getReference(),
                        "threeDS", threeDs
                );
            }

            if ("PURCHASED".equalsIgnoreCase(dto.getState()) || "SUCCESS".equalsIgnoreCase(dto.getState())) {

                return Map.of(
                        "status", dto.getState(),
                        "orderReference", dto.getOrderReference(),
                        "paymentReference", dto.getReference(),
                        "amount", dto.getAmount()
                );
            }

            return dto;

        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt(); // restore interrupt if interrupted
            throw ServiceExceptions.payment("Payment gateway request failed", exception);
        }
    }

    /**
     * Get NGeniusThreeDSResponseDTO
     *
     * @param nGeniusThreeDSRequestDto NGeniusThreeDSResponseDTO
     * @return NGeniusThreeDSResponseDTO
     */
    private NGeniusThreeDSResponseDto sendThreeDSChallenge(NGeniusThreeDSRequestDto nGeniusThreeDSRequestDto, String orderReference, String paymentReference) {

        try {
            NGeniusAccessTokenDto token = getAccessToken();
            String bearerToken = "Bearer " + token.getAccessToken();

            String json = mapper.writeValueAsString(nGeniusThreeDSRequestDto);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/transactions/outlets/" + outletReference + "/orders/" + orderReference + "/payments/" + paymentReference + "/card/3ds2/authentications"))
                    .header("Authorization", bearerToken)
                    .header("Accept", "application/vnd.ni-payment.v2+json")
                    .header("Content-Type", "application/vnd.ni-payment.v2+json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) {
                throw ServiceExceptions.payment("Payment processing failed. Please try again.");
            }

            return mapper.readValue(response.body(), NGeniusThreeDSResponseDto.class);
        } catch (Exception exception) {
            Thread.currentThread().interrupt(); // restore interrupt if interrupted
            throw ServiceExceptions.payment("Failed to obtain payment gateway access token", exception);
        }
    }

    /**
     *
     * @param action NGeniusActionType default PURCHASED
     * @param currencyCode Default AED
     * @param price integer
     * @param customerEmail Customer email
     * @param cardDto CardDto
     * @return NGeniusCreateOrderDto response
     */
    private NGeniusOneStageCreateOrderDto mapCreateOrderDto(NGeniusActionType action, String currencyCode, int price, String customerEmail, CardDto cardDto) {
        NGeniusOneStageCreateOrderDto req = new NGeniusOneStageCreateOrderDto();

        NGeniusOneStageCreateOrderDto.Order order = new NGeniusOneStageCreateOrderDto.Order();
        order.setAction(String.valueOf(action));

        NGeniusOneStageCreateOrderDto.Order.Amount amount = new NGeniusOneStageCreateOrderDto.Order.Amount();
        amount.setCurrencyCode(currencyCode);
        amount.setValue(price);
        order.setAmount(amount);

        order.setEmailAddress(customerEmail);

        NGeniusOneStageCreateOrderDto.Order.MerchantAttributes attrs = new NGeniusOneStageCreateOrderDto.Order.MerchantAttributes();
        attrs.setRedirectUrl(redirectUrl);
        attrs.setSkipConfirmationPage(false);
        attrs.setSkip3DS(false);
        attrs.setCancelUrl(cancelUrl);

        order.setMerchantAttributes(attrs);

        NGeniusOneStageCreateOrderDto.Payment payment = new NGeniusOneStageCreateOrderDto.Payment();
        payment.setPan(cardDto.getPan());
        payment.setExpiry(cardDto.getExpiry());
        payment.setCvv(cardDto.getCvv());
        payment.setCardholderName(cardDto.getCardholderName());
        req.setOrder(order);
        req.setPayment(payment);

        return req;
    }
}
