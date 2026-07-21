package com.aerionsoft.application.service.flight;

import com.aerionsoft.application.dto.flight.search.v1.SearchRequestV1;
import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.dto.flight.MarkupContext;
import com.aerionsoft.application.dto.flight.RecentSearchResponse;
import com.aerionsoft.application.dto.flight.search.Request;
import com.aerionsoft.application.dto.flight.search.Response;
import com.aerionsoft.application.entity.Airport;
import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.entity.FlightSearchLog;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.flight.FlightSearchLogRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.common.AirportRepository;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.service.booking.BookingTimelineService;
import com.aerionsoft.application.service.business.BusinessProviderService;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    @Getter
    @Value("${flight_api_key}")
    private String apiKey;

    @Getter
    @Value("${flight_api_url}")
    private String apiUrl;

    private final WebClient webClient;

    @Autowired
    public SearchService(WebClient insecureWebClient) {
        this.webClient = insecureWebClient;
    }

    @Autowired
    private MarkupService markupService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private FlightSearchLogRepository flightSearchLogRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private BusinessProviderService businessProviderService;

    @Autowired
    private BookingTimelineService bookingTimelineService;

    public Flux<Response> aggregate(String sessionId, Request req,String finalUserIp, String finalUserAgent) {


        if(req.getDepartureDate() == null || req.getDepartureDate().isEmpty() || req.getOrigin() == null || req.getDestination() == null) {
            return Flux.empty();
        }


        String url = apiUrl + "/api/flights/search?sessionId=" + sessionId;

        // Extract markup context from security context
        MarkupContext markupContext = extractMarkupContext();

        if(markupContext.isAgent())
        {
            log.debug("Search request from agent userId={}, businessId={}", markupContext.getUserId(), markupContext.getBusinessId());
            req.setManualCombination(true);

            // Inject allowed providers for this agency
            List<Provider> agencyProviders = businessProviderService.getProviders(markupContext.getBusinessId());
            if (!agencyProviders.isEmpty()) {
                req.setProviders(agencyProviders.stream().map(Provider::getValue).toList());
                log.debug("Injecting providers for business {}: {}", markupContext.getBusinessId(), req.getProviders());
            } else {
                req.setProviders(null);
            }
        }

        AtomicInteger resultCount = new AtomicInteger(0);

        return webClient.post()
                .uri(url)
                .header("x-api-key", apiKey)
                .header(HttpHeaders.ACCEPT, "application/x-ndjson")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToFlux(Response.class)
                // switch execution context for CPU-bound work
                .publishOn(Schedulers.parallel())
                .map(response -> {
                    markupService.applyMarkup(sessionId, response, markupContext);
                    resultCount.incrementAndGet();
                    return response;
                })
                .doOnNext(response -> markupService.storeSearchResponse(response.getResultIndex(), response))
                .doOnComplete(() -> {
                    logFlightSearchAsync(req, markupContext, resultCount.get(), finalUserIp, finalUserAgent);
                    String route = req.getOrigin() + " -> " + req.getDestination();
                    bookingTimelineService.recordFlightStep(sessionId, null, null, BookingStatus.SEARCH, true,
                            route + " (" + resultCount.get() + " results)");
                })
                .doOnError(e -> bookingTimelineService.recordFlightStep(sessionId, null, null,
                        BookingStatus.SEARCH_FAILED, false, e.getMessage()));

    }


    /**
     * Extract MarkupContext from the current security context.
     * Works for authenticated users, agents, and anonymous/guest users.
     */
    private MarkupContext extractMarkupContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            // Guest/anonymous user
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
                User user = getUserByEmail(email);
                if (user != null) {

                    if (user.isAgency()) {
                        if(user.getBusiness() == null) {
                            Optional<BusinessEntity> businessOpt = businessRepository.findFirstByMotherUser(user);
                            businessOpt.ifPresent(user::setBusiness);
                        }
                        return MarkupContext.agent(user.getId(), user.getBusiness().getId());
                    } else {
                        // Regular authenticated user
                        return MarkupContext.authenticatedUser(user.getId());
                    }
                }
            }
        } catch (Exception e) {
            // Log and fallback to guest
        }

        return MarkupContext.guest();
    }

    @Cacheable(value = "userData", key = "#email")
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * Returns the most recent unique flight searches for the given user.
     * Uniqueness is determined by (originCode, destinationCode, departureDate,
     * returnDate, cabinClass, adults, children, infants).
     *
     * @param userId user ID
     * @param limit  maximum number of unique results to return (default 10)
     */
    public List<RecentSearchResponse> getRecentSearches(Long userId, int limit) {
        // Fetch a larger pool so we can deduplicate and still return `limit` unique entries
        List<FlightSearchLog> logs =
                flightSearchLogRepository.findByUserIdOrderBySearchedAtDesc(userId, PageRequest.of(0, limit * 5));

        // Deduplicate: keep only the first (most recent) occurrence of each unique search key
        LinkedHashMap<String, RecentSearchResponse> seen = new LinkedHashMap<>();
        for (FlightSearchLog log : logs) {
            String key = log.getOriginCode() + "|" + log.getDestinationCode() + "|"
                    + log.getDepartureDate() + "|" + (log.getReturnDate() != null ? log.getReturnDate() : "")
                    + "|" + log.getCabinClass()
                    + "|" + log.getAdults() + "|" + log.getChildren() + "|" + log.getInfants();

            seen.computeIfAbsent(key, k -> RecentSearchResponse.builder()
                    .originCode(log.getOriginCode())
                    .originCity(log.getOriginCity())
                    .originCountry(log.getOriginCountry())
                    .destinationCode(log.getDestinationCode())
                    .destinationCity(log.getDestinationCity())
                    .destinationCountry(log.getDestinationCountry())
                    .departureDate(log.getDepartureDate())
                    .returnDate(log.getReturnDate())
                    .tripType(log.getTripType())
                    .cabinClass(log.getCabinClass())
                    .adults(log.getAdults())
                    .children(log.getChildren())
                    .infants(log.getInfants())
                    .resultCount(log.getResultCount())
                    .searchedAt(log.getSearchedAt())
                    .build());

            if (seen.size() == limit) break;
        }

        return new java.util.ArrayList<>(seen.values());
    }

    /**
     * Asynchronously log flight search for analytics.
     * Retrieves airport details (city, country) and user info.
     */
    @Async
    public void logFlightSearchAsync(Request request, MarkupContext markupContext, int resultCount,
                                      String userIp, String userAgent) {
        try {
            // Get airport details from database
            Airport originAirport = airportRepository.findByCode(request.getOrigin()).orElse(null);
            Airport destAirport = airportRepository.findByCode(request.getDestination()).orElse(null);


            // Determine trip type
            String tripType = (request.getReturnDate() != null && !request.getReturnDate().isEmpty())
                ? "ROUNDTRIP" : "ONEWAY";

            // Map cabin class
            String cabinClass = getCabinClassName(request.getCabinClass());

            // Build and save the log
            FlightSearchLog searchLog = FlightSearchLog.builder()
                .originCode(request.getOrigin())
                .originCity(originAirport != null ? originAirport.getCityName() : null)
                .originCountry(originAirport != null ? originAirport.getCountryName() : null)
                .destinationCode(request.getDestination())
                .destinationCity(destAirport != null ? destAirport.getCityName() : null)
                .destinationCountry(destAirport != null ? destAirport.getCountryName() : null)
                .departureDate(request.getDepartureDate())
                .returnDate(request.getReturnDate())
                .tripType(tripType)
                .cabinClass(cabinClass)
                .adults(request.getAdults())
                .children(request.getChildren())
                .infants(request.getInfants())
                .userId(markupContext.getUserId())
                .userIp(userIp)
                .userAgent(userAgent)
                .searchedAt(UserDateTimeUtil.now())
                .resultCount(resultCount)
                .build();

            flightSearchLogRepository.save(searchLog);
            log.debug("Flight search logged: {} -> {} ({} results)",
                request.getOrigin(), request.getDestination(), resultCount);
        } catch (Exception e) {
            log.error("Failed to log flight search", e);
        }
    }

    /**
     * Map cabin class code to name.
     */
    private String getCabinClassName(int cabinClass) {
        return switch (cabinClass) {
            case 1 -> "ECONOMY";
            case 2 -> "PREMIUM_ECONOMY";
            case 3 -> "BUSINESS";
            case 4 -> "FIRST";
            default -> "ECONOMY";
        };
    }

    /**
     * V1 Search - passes v1 request format as-is to the backend API
     * Handles multiple origin-destination pairs in a single request
     */
    public Flux<Response> aggregateV1(String sessionId, SearchRequestV1 requestV1,
                                      String finalUserIp, String finalUserAgent) {

        String url = apiUrl + "/api/flights/v1/search?sessionId=" + sessionId;

        // Extract markup context from security context
        MarkupContext markupContext = extractMarkupContext();

        // Counter for result count
        AtomicInteger resultCount = new AtomicInteger(0);

        // Inject allowed providers for agent searches (V1)
        if (markupContext.isAgent()) {
            List<Provider> agencyProviders = businessProviderService.getProviders(markupContext.getBusinessId());
            if (!agencyProviders.isEmpty()) {
                requestV1.setProviders(agencyProviders.stream().map(Provider::getValue).toList());
                log.debug("V1 – injecting providers for business {}: {}", markupContext.getBusinessId(), requestV1.getProviders());
            } else {
                requestV1.setProviders(null);
            }
        }

        return webClient.post()
                .uri(url)
                .header("x-api-key", apiKey)
                .header(HttpHeaders.ACCEPT, "application/x-ndjson")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestV1)
                .retrieve()
                .bodyToFlux(Response.class)
                // switch execution context for CPU-bound work
                .publishOn(Schedulers.parallel())
                .map(response -> {
                    markupService.applyMarkup(sessionId, response, markupContext);
                    resultCount.incrementAndGet();
                    return response;
                })
                .doOnNext(response -> markupService.storeSearchResponse(response.getResultIndex(), response))
                .doOnComplete(() -> {
                    logFlightSearchV1Async(requestV1, markupContext, resultCount.get(), finalUserIp, finalUserAgent);
                    int odCount = requestV1.getOriginDestinations() != null ? requestV1.getOriginDestinations().size() : 0;
                    bookingTimelineService.recordFlightStep(sessionId, null, null, BookingStatus.SEARCH, true,
                            "V1 search (" + odCount + " segments, " + resultCount.get() + " results)");
                })
                .doOnError(e -> bookingTimelineService.recordFlightStep(sessionId, null, null,
                        BookingStatus.SEARCH_FAILED, false, e.getMessage()));
    }

    @Async
    public void logFlightSearchV1Async(SearchRequestV1 requestV1,
                                       MarkupContext markupContext, int resultCount, String userIp, String userAgent) {
        try {
            // Log multi-city search
            String routes = String.join(", ",
                requestV1.getOriginDestinations().stream()
                    .map(od -> od.getOrigin() + "-" + od.getDestination())
                    .toList()
            );

            log.debug("V1 Flight search logged: {} ({} results, {} origin-destinations)",
                routes, resultCount, requestV1.getOriginDestinations().size());
        } catch (Exception e) {
            log.error("Failed to log V1 flight search", e);
        }
    }
}
