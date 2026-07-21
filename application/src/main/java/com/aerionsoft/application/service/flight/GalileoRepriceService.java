package com.aerionsoft.application.service.flight;

import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.dto.flight.MarkupContext;
import com.aerionsoft.application.dto.flight.galileo.RepriceRequest;
import com.aerionsoft.application.dto.flight.search.Response;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.repository.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class GalileoRepriceService {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    private final WebClient webClient;
    private final MarkupService markupService;
    private final UserRepository userRepository;

    @Value("${flight_api_key}")
    private String apiKey;

    @Value("${flight_api_url}")
    private String apiUrl;

    public GalileoRepriceService(WebClient insecureWebClient,
                                 MarkupService markupService,
                                 UserRepository userRepository) {
        this.webClient = insecureWebClient;
        this.markupService = markupService;
        this.userRepository = userRepository;
    }

    public Response reprice(String sessionId, RepriceRequest request, String traceId) {
        String url = apiUrl + "/api/flights/reprice?sessionId=" + sessionId;
        String resolvedTraceId = resolveTraceId(traceId != null ? traceId : sessionId);

        log.info("[Session: {}] Calling reprice for resultIndex: {}, channel: {}, traceId: {}",
                sessionId, request.getResultIndex(), request.getChannel(), resolvedTraceId);

        Response response = webClient.post()
                .uri(url)
                .header("x-api-key", apiKey)
                .header(TRACE_ID_HEADER, resolvedTraceId)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Response.class)
                .block();

        if (response == null) {
            log.error("Galileo reprice returned null for resultIndex: {}", request.getResultIndex());
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Reprice request returned empty response");
        }

        if (isFailed(response)) {
            log.warn("Galileo reprice failed for resultIndex {}: {} ({})",
                    request.getResultIndex(), response.getMessage(), response.getReason());
            return response;
        }

        MarkupContext context = extractMarkupContext();
        markupService.applyMarkup(sessionId, response, context, toMarkupSelections(request));
        markupService.storeSearchResponse(response.getResultIndex(), response);

        log.info("[Session: {}] Galileo reprice succeeded for resultIndex: {} -> {}, isPriceChanged: {}",
                sessionId, request.getResultIndex(), response.getResultIndex(), response.getIsPriceChanged());

        return response;
    }

    private List<MarkupService.MarkupSegmentSelection> toMarkupSelections(RepriceRequest request) {
        return request.getBookingClasses().stream()
                .map(selection -> new MarkupService.MarkupSegmentSelection(
                        selection.getLeg(),
                        selection.getBookingCode()))
                .toList();
    }

    private static boolean isFailed(Response response) {
        return "FAILED".equalsIgnoreCase(response.getStatus());
    }

    private static String resolveTraceId(String traceId) {
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return UUID.randomUUID().toString();
    }

    private MarkupContext extractMarkupContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return MarkupContext.guest();
        }

        try {
            Object principal = authentication.getPrincipal();
            String email = null;

            if (principal instanceof UserDetails) {
                email = ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                email = (String) principal;
            }

            if (email != null) {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    if (user.isAgency() && user.getBusiness() != null) {
                        return MarkupContext.agent(user.getId(), user.getBusiness().getId());
                    }
                    return MarkupContext.authenticatedUser(user.getId());
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting markup context: {}", e.getMessage());
        }

        return MarkupContext.guest();
    }
}
