package com.aerionsoft.application.service.flight;

import com.aerionsoft.application.dto.flight.search.extras.*;
import com.aerionsoft.application.dto.flight.search.extras.Airport;
import com.aerionsoft.application.entity.*;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.MarkupPlanBusinessResponse;
import com.aerionsoft.application.dto.flight.MarkupCombinedCondition;
import com.aerionsoft.application.dto.flight.MarkupContext;
import com.aerionsoft.application.dto.flight.search.Response;
import com.aerionsoft.application.dto.flight.search.extras.FinalFare;
import com.aerionsoft.application.enums.flight.BangladeshAirport;
import com.aerionsoft.application.enums.flight.MarkupFilterMode;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.flight.MarkupLogRepository;
import com.aerionsoft.application.repository.flight.MarkupPlanBusinessRepository;
import com.aerionsoft.application.repository.flight.MarkupPlanRepository;
import com.aerionsoft.application.repository.flight.MarkupRuleRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.aerionsoft.application.util.TimestampMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MarkupService {

    private static final Logger log = LoggerFactory.getLogger(MarkupService.class);

    /**
     * High-performance Caffeine cache for markup data.
     * - Supports 10K+ TPS with minimal latency
     * - Auto-expiry after 15 minutes
     * - Max 100K entries to prevent memory issues
     * - Lock-free reads for maximum throughput
     */
    private static final Cache<String, MarkupData> markupCache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(Duration.ofMinutes(15))
            .build();

    /**
     * Cache for markup rules to avoid repeated database queries.
     * - Expires after 5 minutes to balance performance and freshness
     * - Key format: userType_businessId or "GUEST_null"
     */
    private static final Cache<String, List<MarkupRule>> rulesCache = Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfterWrite(Duration.ofMinutes(15))
            .build();

    /**
     * Cache for full search Response objects, keyed by resultIndex.
     * Used by SSRService to retrieve segment/airline/route data for isRuleApplicable checks.
     * - 15 min TTL matches markupCache TTL
     * - Max 100K entries
     */
    private static final Cache<String, Response> searchResponseCache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(Duration.ofMinutes(15))
            .build();

    @Autowired
    private MarkupPlanRepository markupPlanRepository;

    @Autowired
    private MarkupRuleRepository markupRuleRepository;

    @Autowired
    private MarkupLogRepository markupLogRepository;


    @Autowired
    private MarkupPlanBusinessRepository markupPlanBusinessRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private TimestampMapper timestampMapper;

    public MarkupPlan createMarkupPlan(MarkupPlan plan) {
        return markupPlanRepository.save(plan);
    }

    public MarkupPlan getMarkupPlan(Long id) {
        return markupPlanRepository.findById(id).orElse(null);
    }

    public List<MarkupPlan> getAllMarkupPlans() {
        return markupPlanRepository.findAll();
    }

    @Transactional
    public void deleteMarkupPlan(Long id) {
        // Find and invalidate caches for all businesses bound to this plan
        List<MarkupPlanBusiness> bindings = markupPlanBusinessRepository.findAllByMarkupPlanId(id);
        for (MarkupPlanBusiness binding : bindings) {
            rulesCache.invalidate("AGENT_" + binding.getBusinessId());
        }

        markupPlanRepository.deleteById(id);
        log.info("Deleted markup plan {} and invalidated {} business caches", id, bindings.size());
    }

    public MarkupRule addMarkupRule(MarkupRule rule) {
        // Fetch the full MarkupPlan if only ID is provided
        if (rule.getMarkupPlan() != null && rule.getMarkupPlan().getId() != null) {
            MarkupPlan fullPlan = markupPlanRepository.findById(rule.getMarkupPlan().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("MarkupPlan", rule.getMarkupPlan().getId()));
            rule.setMarkupPlan(fullPlan);
        }
        if (rule.getFilterMode() == null) {
            rule.setFilterMode(MarkupFilterMode.INDIVIDUAL);
        }
        normalizeFilterModeFields(rule);
        validateRuleFilters(rule);
        MarkupRule savedRule = markupRuleRepository.save(rule);

        // Invalidate cache for affected businesses
        invalidateCacheForRule(savedRule);

        return savedRule;
    }

    public MarkupPlan updateMarkupPlan(Long id, MarkupPlan plan) {
        MarkupPlan existingPlan = markupPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MarkupPlan", id));

        if (plan.getTitle() != null) {
            existingPlan.setTitle(plan.getTitle());
        }
        if (plan.getAitType() != null) {
            existingPlan.setAitType(plan.getAitType());
        }
        if (plan.getAitValue() != null) {
            existingPlan.setAitValue(plan.getAitValue());
        }
        if (plan.getTargetUserType() != null) {
            existingPlan.setTargetUserType(plan.getTargetUserType());
        }

        return markupPlanRepository.save(existingPlan);
    }

    public List<MarkupRule> getRulesByPlanId(Long planId) {
        return markupRuleRepository.findByMarkupPlan_IdOrderByPriorityDesc(planId);
    }

    public MarkupRule updateMarkupRule(Long id, MarkupRule rule) {
        MarkupRule existingRule = markupRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MarkupRule", id));

        // Update fields if provided
        if (rule.getProvider() != null) existingRule.setProvider(rule.getProvider());
        if (rule.getOrigin() != null) existingRule.setOrigin(rule.getOrigin());
        if (rule.getPriority() != null) existingRule.setPriority(rule.getPriority());
        if (rule.getAirlineCode() != null) existingRule.setAirlineCode(rule.getAirlineCode());
        if (rule.getAirlineName() != null) existingRule.setAirlineName(rule.getAirlineName());
        if (rule.getIsSuspended() != null) existingRule.setIsSuspended(rule.getIsSuspended());
        if (rule.getFlyType() != null) existingRule.setFlyType(rule.getFlyType());
        if (rule.getAppliedOn() != null) existingRule.setAppliedOn(rule.getAppliedOn());
        if (rule.getStartDate() != null) existingRule.setStartDate(rule.getStartDate());
        if (rule.getEndDate() != null) existingRule.setEndDate(rule.getEndDate());
        if (rule.getRoutes() != null) existingRule.setRoutes(rule.getRoutes());
        if (rule.getBookingCodes() != null) existingRule.setBookingCodes(rule.getBookingCodes());
        if (rule.getFilterMode() != null) existingRule.setFilterMode(rule.getFilterMode());
        if (rule.getCombinedConditions() != null) existingRule.setCombinedConditions(rule.getCombinedConditions());
        if (rule.getMinBaseFare() != null) existingRule.setMinBaseFare(rule.getMinBaseFare());
        if (rule.getMaxBaseFare() != null) existingRule.setMaxBaseFare(rule.getMaxBaseFare());
        if (rule.getIsActive() != null) existingRule.setIsActive(rule.getIsActive());
        if (rule.getCommissionProvision() != null) existingRule.setCommissionProvision(rule.getCommissionProvision());
        if (rule.getCommissionLessApplied() != null)
            existingRule.setCommissionLessApplied(rule.getCommissionLessApplied());
        if (rule.getCommissionType() != null) existingRule.setCommissionType(rule.getCommissionType());
        if (rule.getMarkupValue() != null) existingRule.setMarkupValue(rule.getMarkupValue());
        if (rule.getMarkupType() != null) existingRule.setMarkupType(rule.getMarkupType());

        // Update markup plan if provided
        if (rule.getMarkupPlan() != null && rule.getMarkupPlan().getId() != null) {
            MarkupPlan fullPlan = markupPlanRepository.findById(rule.getMarkupPlan().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("MarkupPlan", rule.getMarkupPlan().getId()));
            existingRule.setMarkupPlan(fullPlan);
        }

        normalizeFilterModeFields(existingRule);
        validateRuleFilters(existingRule);
        MarkupRule savedRule = markupRuleRepository.save(existingRule);

        // Invalidate cache for affected businesses
        invalidateCacheForRule(savedRule);

        return savedRule;
    }

    @Transactional
    public void deleteMarkupRule(Long id) {
        markupRuleRepository.findById(id).ifPresent(this::invalidateCacheForRule);
        markupRuleRepository.deleteById(id);
    }

    /**
     * Invalidate cache for all businesses affected by a rule change.
     */
    private void invalidateCacheForRule(MarkupRule rule) {
        if (rule.getMarkupPlan() != null) {
            List<MarkupPlanBusiness> bindings = markupPlanBusinessRepository
                    .findAllByMarkupPlanId(rule.getMarkupPlan().getId());
            for (MarkupPlanBusiness binding : bindings) {
                rulesCache.invalidate("AGENT_" + binding.getBusinessId());
            }
        }
    }

    /**
     * Apply markup rules based on user context.
     * This method is called for both authenticated and unauthenticated users.
     * For agents, rules are aggregated from all active markup plans bound to their business.
     * Also applies markup to FarePackages (for USBANGLAAPI bundle selection).
     */
    public void applyMarkup(String sessionId, Response response, MarkupContext context) {
        applyMarkup(sessionId, response, context, List.of());
    }

    public void applyMarkup(String sessionId, Response response, MarkupContext context,
                            List<MarkupSegmentSelection> segmentSelections) {
        List<MarkupRule> rules = getApplicableRules(context);

        if (response.getFare() != null) {
            applyRuleToFare(sessionId, response, response.getFare(), rules, context, segmentSelections);
        }

        // Apply markup to FarePackages if present (for USBANGLAAPI bundle selection)
        if (response.getFarePackages() != null && response.getFarePackages().length > 0) {
            applyMarkupToFarePackages(sessionId, response, rules, context);
        }
    }

    /**
     * Get applicable markup rules based on user context with caching.
     * For agents with businesses, this aggregates rules from all active markup plans
     * bound to the business and returns them sorted by priority.
     */
    public List<MarkupRule> getApplicableRules(MarkupContext context) {
        String cacheKey = context.getUserType() + "_" +
                (context.getBusinessId() != null ? context.getBusinessId() : "null");

        // Try to get from cache first
        List<MarkupRule> cachedRules = rulesCache.getIfPresent(cacheKey);
        if (cachedRules != null) {
            return cachedRules;
        }

        // Fetch from database
        List<MarkupRule> rules;
        if (context.isAgent() && context.getBusinessId() != null) {
            rules = markupRuleRepository.findByBusinessIdOrderByPriorityDesc(context.getBusinessId());
            log.debug("Loaded {} rules for business {} from database", rules.size(), context.getBusinessId());
        } else {
            rules = markupRuleRepository.findByUserTypeOrderByPriorityDesc("GUEST");
            log.debug("Loaded {} rules for GUEST from database", rules.size());
        }

        // Store in cache
        rulesCache.put(cacheKey, rules);
        return rules;
    }


    private void applyRuleToFare(String sessionId, Response response, FinalFare fare, List<MarkupRule> rules,
                                 MarkupContext context, List<MarkupSegmentSelection> segmentSelections) {
        for (MarkupRule rule : rules) {
            if (isRuleApplicable(rule, response, fare, segmentSelections)) {
                MarkupCalculation calc = calculateMarkup(fare.getBaseFare(), fare.getTax(), rule);

                double originalPublished = fare.getPublishedFare();
                double newOfferFare = calc.offerFare + calc.ait + calc.markupAmount;
                double newPublishedFare = originalPublished + calc.markupAmount + calc.ait;

                String bindinfo = String.format("Markup Applied: Rule ID %d, Markup: %.2f, AIT: %.2f, Commission: %.2f",
                        rule.getId(), calc.markupAmount, calc.ait, calc.commission);

                fare.setPublishedFare(newPublishedFare);
                fare.setRemarks(bindinfo);
                fare.setAit(calc.ait);
                fare.setDiscount(fare.getDiscount() + calc.commission);
                if (calc.commission == 0.0) {
                    // No commission discount: UI uses publishedFare. Absorb markup+AIT into
                    // baseFare (and fareBreakDowns) so BaseFare + Tax still equals the fare.
                    double addOn = calc.markupAmount + calc.ait;
                    if (addOn != 0.0) {
                        fare.setBaseFare(fare.getBaseFare() + addOn);
                        applyAddOnToFareBreakDowns(response.getFareBreakDowns(), addOn);
                    }
                    fare.setOfferFare(null);
                } else {
                    fare.setOfferFare(newOfferFare);
                }

                storeMarkupData(response.getResultIndex(), originalPublished, calc.totalMarkup, newPublishedFare, calc.buyPrice());
                break; // Apply only the highest priority rule
            }
        }
    }

    /**
     * Distributes {@code addOn} across fare-breakdown BaseFare values (proportional to current
     * base share) so per-pax breakdowns stay consistent with the total fare base.
     */
    private void applyAddOnToFareBreakDowns(List<FareBreakDown> fareBreakDowns, double addOn) {
        if (fareBreakDowns == null || fareBreakDowns.isEmpty() || addOn == 0.0) {
            return;
        }

        double totalBase = 0.0;
        for (FareBreakDown breakdown : fareBreakDowns) {
            totalBase += breakdown.getBaseFare();
        }

        if (totalBase <= 0.0) {
            FareBreakDown first = fareBreakDowns.get(0);
            first.setBaseFare(first.getBaseFare() + addOn);
            return;
        }

        double applied = 0.0;
        for (int i = 0; i < fareBreakDowns.size(); i++) {
            FareBreakDown breakdown = fareBreakDowns.get(i);
            if (i == fareBreakDowns.size() - 1) {
                // Last row gets remainder to avoid floating-point drift
                breakdown.setBaseFare(breakdown.getBaseFare() + (addOn - applied));
            } else {
                double share = addOn * (breakdown.getBaseFare() / totalBase);
                breakdown.setBaseFare(breakdown.getBaseFare() + share);
                applied += share;
            }
        }
    }

    /**
     * Calculate markup, AIT, and commission for a given base fare.
     * Extracted to reduce code duplication between fare and fare package markup.
     */
    public MarkupCalculation calculateMarkup(double baseFare, double tax, MarkupRule rule) {
        double commission = 0.0;
        if (rule.getCommissionLessApplied() != null) {
            commission = calculateAmount(baseFare, rule.getCommissionType(), rule.getCommissionLessApplied());
        }

        double commissionProvision = 0.0;
        if (rule.getCommissionProvision() != null) {
            commissionProvision = calculateAmount(baseFare, rule.getCommissionType(), rule.getCommissionProvision());
        }

        double offerFare = (baseFare - commission) + tax;

        // AIT from Plan
        double ait = 0.0;
        if (rule.getMarkupPlan() != null && rule.getMarkupPlan().getAitValue() != null) {
            ait = calculateAmount(offerFare, rule.getMarkupPlan().getAitType(), rule.getMarkupPlan().getAitValue());
        }

        double buyPrice = (baseFare - commissionProvision) + tax + ait;

        double markupAmount = calculateAmount(offerFare, rule.getMarkupType(), rule.getMarkupValue());
        double totalMarkup = markupAmount + ait - commission;

        return new MarkupCalculation(commission, offerFare, ait, markupAmount, totalMarkup, buyPrice);
    }

    /**
     * Record to hold markup calculation results.
     */
    public record MarkupCalculation(double commission, double offerFare, double ait, double markupAmount,
                                    double totalMarkup, double buyPrice) {
    }

    public record MarkupSegmentSelection(Integer leg, String bookingCode) {
    }

    /**
     * Apply markup to FarePackages (for USBANGLAAPI bundle selection).
     * Each package's offerFare gets markup applied and stored separately with bundleCode.
     */
    private void applyMarkupToFarePackages(String sessionId, Response response, List<MarkupRule> rules, MarkupContext context) {
        if (response.getFarePackages() == null || response.getFarePackages().length == 0) {
            return;
        }

        for (int i = 0; i < response.getFarePackages().length; i++) {
            var farePackage = response.getFarePackages()[i];

            // Skip if no baseFare to markup
            if (farePackage.getPackageFare() == null || farePackage.getPackageFare().getBaseFare() == null) {
                continue;
            }

            for (MarkupRule rule : rules) {
                if (isRuleApplicable(rule, response, null)) {
                    try {
                        double tax = farePackage.getPackageFare().getTax() != null ? farePackage.getPackageFare().getTax() : 0.0;
                        double baseFare = Double.parseDouble(farePackage.getPrice()) - tax;
                        double originalPrice = baseFare + tax;

                        MarkupCalculation calc = calculateMarkup(baseFare, tax, rule);

                        // Apply markup to offerFare
                        double newOfferFare = calc.offerFare + calc.ait + calc.markupAmount;
                        farePackage.setOfferFare(newOfferFare);
                        String bundleCode = farePackage.getId();
                        if (bundleCode != null && !bundleCode.isBlank()) {
                            String cacheKey = getCacheKey(response, bundleCode, farePackage);
                            storeMarkupData(cacheKey, originalPrice, calc.totalMarkup, newOfferFare, calc.buyPrice());
//
//                            log.debug("[Session: {}] Applied markup to FarePackage[{}] bundleCode={}, original={}, markup={}, final={}",
//                                    sessionId, i, bundleCode, originalPrice, calc.totalMarkup, newOfferFare);
                        }

                    } catch (Exception e) {
                        log.warn("[Session: {}] Failed to apply markup to FarePackage[{}]: {}", sessionId, i, e.getMessage());
                    }

                    break; // Apply only the highest priority rule
                }
            }
        }
    }

    private String getCacheKey(Response response, String bundleCode, FarePackage farePackage) {
        String cacheKey = response.getResultIndex() + ":" + bundleCode;
        if (response.getType().equalsIgnoreCase("USBANGLAAPI") || response.getType().equalsIgnoreCase("VERTEIL")
        ) {
            cacheKey = response.getResultIndex() + ":outbound:" + bundleCode;
        }
        if (response.getType().equalsIgnoreCase("FLYDUBAI")) {
            cacheKey = response.getResultIndex() + ":outbound:" + bundleCode + ":" + farePackage.getTitle();
        }
        return cacheKey;
    }


    public void storeMarkupData(String resultIndex, double originalPrice, double totalMarkup, Double bookingPrice) {
        storeMarkupData(resultIndex, originalPrice, totalMarkup, bookingPrice, null);
    }

    public void storeMarkupData(String resultIndex, double originalPrice, double totalMarkup, Double bookingPrice,
                                Double buyPrice) {
        if (resultIndex != null && !resultIndex.isBlank()) {
            Double resolvedBuyPrice = buyPrice != null ? buyPrice : originalPrice;
            markupCache.put(resultIndex, new MarkupData(originalPrice, totalMarkup, bookingPrice, resolvedBuyPrice));
        }
    }

    /**
     * Get stored markup data from Caffeine cache.
     * Returns null if not found or expired (auto-handled by Caffeine).
     * Can be called with resultIndex or resultIndex:bundleCode combination.
     */
    public MarkupData getStoredMarkupData(String resultIndex) {
        return (resultIndex != null && !resultIndex.isBlank()) ? markupCache.getIfPresent(resultIndex) : null;
    }

    /**
     * Store full search Response in cache keyed by resultIndex (15 min TTL).
     * Called by SearchService after applying markup so SSRService can reuse it for isRuleApplicable.
     * Runs asynchronously to avoid blocking the search response stream.
     */
    @Async
    public void storeSearchResponse(String resultIndex, Response response) {
        if (resultIndex != null && !resultIndex.isBlank() && response != null) {
            searchResponseCache.put(resultIndex, response);
        }
    }

    /**
     * Get cached search Response by resultIndex.
     * Returns null if not found or expired.
     */
    public Response getSearchResponse(String resultIndex) {
        return (resultIndex != null && !resultIndex.isBlank()) ? searchResponseCache.getIfPresent(resultIndex) : null;
    }

    /**
     * Holds GDS-side original reference, total markup, and optional final customer price from search/SSR/validation.
     */
    public record MarkupData(double originalPrice, double totalMarkup, Double bookingPrice, Double buyPrice) {
        public MarkupData(double originalPrice, double totalMarkup, Double bookingPrice) {
            this(originalPrice, totalMarkup, bookingPrice, originalPrice);
        }

        public MarkupData(double originalPrice, double totalMarkup) {
            this(originalPrice, totalMarkup, null, originalPrice);
        }

        /**
         * Final price for billing: explicit {@link #bookingPrice} when present, otherwise original + markup.
         */
        public double resolvedBookingPrice() {
            double raw = bookingPrice != null ? bookingPrice : originalPrice + totalMarkup;
            return BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        /**
         * Supplier buy price: explicit {@link #buyPrice} when present, otherwise GDS original price.
         */
        public double resolvedBuyPrice() {
            double raw = buyPrice != null ? buyPrice : originalPrice;
            return BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
    }

    public boolean isRuleApplicable(MarkupRule rule, Response response, FinalFare fare) {
        return isRuleApplicable(rule, response, fare, List.of());
    }

    public boolean isRuleApplicable(MarkupRule rule, Response response, FinalFare fare,
                                    List<MarkupSegmentSelection> segmentSelections) {
        if (response.getSegments() == null || response.getSegments().isEmpty()) {
            return false; // Cannot apply rules without segment info
        }

        // 1. Provider Check
        if (rule.getProvider() != null && !rule.getProvider().equalsIgnoreCase("Any")) {
            if (response.getChannel() == null || !response.getChannel().equalsIgnoreCase(rule.getProvider())) {
                return false;
            }
        }

        Segments firstSegment = response.getSegments().get(0);

        // 2. Origin Check (BD, NBD, Any) — based on the itinerary origin (first segment).
        if (!matchesOriginRule(rule, firstSegment)) {
            return false;
        }

        // 3–5. Route / airline / booking code — individual OR combined (mutually exclusive modes).
        if (isCombinedFilterMode(rule)) {
            // Class-combination rules describe a single direction, so they never apply to a
            // round trip; those itineraries fall through to a route/individual rule instead.
            if (isRoundTrip(response)) {
                return false;
            }
            // For a one-way itinerary, every flown segment must be covered by a condition.
            for (Segments segment : response.getSegments()) {
                String bookingOverride = bookingCodeOverrideFor(segment, segmentSelections);
                if (!matchesAnyCombinedCondition(rule, new SegmentCandidate(segment, bookingOverride))) {
                    return false;
                }
            }
        } else {
            String bookingOverride = bookingCodeOverrideFor(firstSegment, segmentSelections);
            if (!matchesAirlineRule(rule, firstSegment)
                    || !matchesBookingCodeRule(rule, firstSegment, bookingOverride)
                    || !matchesRouteRule(rule, firstSegment)) {
                return false;
            }
        }

        // 6. Date Check (Applied On)
        if ("Specific Time Period".equalsIgnoreCase(rule.getAppliedOn())) {
            java.time.LocalDate now = java.time.LocalDate.now();
            if (rule.getStartDate() != null && now.isBefore(rule.getStartDate())) {
                return false;
            }
            if (rule.getEndDate() != null && now.isAfter(rule.getEndDate())) {
                return false;
            }
        }

        // 7. Min/Max Base Fare Check
        if (fare != null) {
            if (rule.getMinBaseFare() != null && rule.getMinBaseFare() > 0 && fare.getBaseFare() < rule.getMinBaseFare()) {
                return false;
            }
            return rule.getMaxBaseFare() == null || rule.getMaxBaseFare() <= 0 || fare.getBaseFare() <= rule.getMaxBaseFare();
        }

        return true;
    }

    /**
     * True when the itinerary is a round trip. Uses the provider's {@code isReturn} flag when
     * present, otherwise falls back to detecting that the itinerary returns to its origin.
     */
    private boolean isRoundTrip(Response response) {
        if (Boolean.TRUE.equals(response.getIsReturn())) {
            return true;
        }
        List<Segments> segments = response.getSegments();
        if (segments == null || segments.size() < 2) {
            return false;
        }
        String itineraryOrigin = airportCodeOf(segments.get(0).getOrigin());
        String itineraryEnd = airportCodeOf(segments.get(segments.size() - 1).getDestination());
        return itineraryOrigin != null && itineraryOrigin.equalsIgnoreCase(itineraryEnd);
    }

    private String airportCodeOf(Location location) {
        if (location == null || location.getAirport() == null) {
            return null;
        }
        return location.getAirport().getAirportCode();
    }

    private boolean matchesOriginRule(MarkupRule rule, Segments segment) {
        if (rule.getOrigin() == null || rule.getOrigin().equalsIgnoreCase("Any")) {
            return true;
        }
        if (segment.getOrigin() == null || segment.getOrigin().getAirport() == null) {
            return false;
        }
        boolean isBangladeshOrigin = isBangladeshOrigin(segment.getOrigin().getAirport());
        if ("BD".equalsIgnoreCase(rule.getOrigin())) {
            return isBangladeshOrigin;
        }
        if ("NBD".equalsIgnoreCase(rule.getOrigin())) {
            return !isBangladeshOrigin;
        }
        return true;
    }

    /**
     * Resolve the booking code chosen for a segment during reprice, matched by leg.
     * Returns {@code null} when there is no selection for that leg (search flow),
     * so the segment's own booking code is used.
     */
    private String bookingCodeOverrideFor(Segments segment, List<MarkupSegmentSelection> segmentSelections) {
        if (segmentSelections == null || segmentSelections.isEmpty()) {
            return null;
        }
        for (MarkupSegmentSelection selection : segmentSelections) {
            if (selection == null) {
                continue;
            }
            Integer selectedLeg = selection.leg();
            if (selectedLeg == null || selectedLeg == segment.getLeg()) {
                return selection.bookingCode();
            }
        }
        return null;
    }

    private record SegmentCandidate(Segments segment, String bookingCodeOverride) {
    }

    private void validateRuleFilters(MarkupRule rule) {
        if (!isCombinedFilterMode(rule)) {
            return;
        }
        if (rule.getCombinedConditions() == null || rule.getCombinedConditions().isEmpty()) {
            throw BusinessException.validation(
                    "combinedConditions is required when filterMode is COMBINED",
                    java.util.Map.of("combinedConditions", "At least one combined condition row is required"));
        }
        for (int i = 0; i < rule.getCombinedConditions().size(); i++) {
            MarkupCombinedCondition condition = rule.getCombinedConditions().get(i);
            if (condition == null || !condition.hasAnyField()) {
                throw BusinessException.validation(
                        "Each combined condition must specify route, airlineCode, and/or bookingCode",
                        java.util.Map.of("combinedConditions[" + i + "]", "At least one field is required"));
            }
        }
    }

    /**
     * Keeps filter fields consistent with {@link MarkupFilterMode}.
     * Switching to INDIVIDUAL clears stored combined rows.
     */
    void normalizeFilterModeFields(MarkupRule rule) {
        if (rule.getFilterMode() == null) {
            rule.setFilterMode(MarkupFilterMode.INDIVIDUAL);
        }
        if (rule.getFilterMode() == MarkupFilterMode.INDIVIDUAL) {
            rule.setCombinedConditions(null);
        }
    }

    private boolean isCombinedFilterMode(MarkupRule rule) {
        return rule.getFilterMode() == MarkupFilterMode.COMBINED;
    }

    private boolean matchesAnyCombinedCondition(MarkupRule rule, SegmentCandidate candidate) {
        if (rule.getCombinedConditions() == null || rule.getCombinedConditions().isEmpty()) {
            return false;
        }
        return rule.getCombinedConditions().stream()
                .anyMatch(condition -> matchesCombinedCondition(condition, candidate));
    }

    private boolean matchesCombinedCondition(MarkupCombinedCondition condition, SegmentCandidate candidate) {
        Segments segment = candidate.segment();
        if (condition.getRoute() != null && !condition.getRoute().isBlank()) {
            if (!segmentMatchesRoute(segment, condition.getRoute())) {
                return false;
            }
        }
        if (condition.getAirlineCode() != null && !condition.getAirlineCode().isBlank()) {
            if (!segmentMatchesAirlineCodes(segment, condition.getAirlineCode(), false)) {
                return false;
            }
        }
        if (condition.getBookingCode() != null && !condition.getBookingCode().isBlank()) {
            if (!segmentMatchesBookingCodes(segment, condition.getBookingCode(), candidate.bookingCodeOverride())) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesAirlineRule(MarkupRule rule, Segments segment) {
        if (rule.getAirlineCode() == null || rule.getAirlineCode().isEmpty()) {
            return true;
        }
        boolean suspended = Boolean.TRUE.equals(rule.getIsSuspended());
        return segmentMatchesAirlineCodes(segment, rule.getAirlineCode(), suspended);
    }

    private boolean matchesBookingCodeRule(MarkupRule rule, Segments segment, String bookingCodeOverride) {
        if (rule.getBookingCodes() == null || rule.getBookingCodes().isEmpty()) {
            return true;
        }
        return segmentMatchesBookingCodes(segment, rule.getBookingCodes(), bookingCodeOverride);
    }

    private boolean matchesRouteRule(MarkupRule rule, Segments segment) {
        if (rule.getRoutes() == null || rule.getRoutes().isEmpty()) {
            return true;
        }
        return segmentMatchesRoute(segment, rule.getRoutes());
    }

    private boolean segmentMatchesAirlineCodes(Segments segment, String airlineCodes, boolean suspended) {
        if (segment.getAirline() == null || segment.getAirline().getAirlineCode() == null) {
            return false;
        }
        String segmentAirline = segment.getAirline().getAirlineCode();
        boolean found = containsIgnoreCase(airlineCodes, segmentAirline);
        return suspended != found;
    }

    private boolean segmentMatchesBookingCodes(Segments segment, String bookingCodes, String bookingCodeOverride) {
        String bookingCode = bookingCodeOverride != null && !bookingCodeOverride.isBlank()
                ? bookingCodeOverride
                : segment.getBookingCode();
        if (bookingCode == null) {
            return false;
        }
        return containsIgnoreCase(bookingCodes, bookingCode);
    }

    private boolean segmentMatchesRoute(Segments segment, String allowedRoutes) {
        if (segment.getOrigin() == null || segment.getOrigin().getAirport() == null
                || segment.getDestination() == null || segment.getDestination().getAirport() == null) {
            return false;
        }
        String route = segment.getOrigin().getAirport().getAirportCode() + "-"
                + segment.getDestination().getAirport().getAirportCode();
        return containsIgnoreCase(allowedRoutes, route);
    }

    private boolean containsIgnoreCase(String commaSeparatedValues, String value) {
        if (value == null) {
            return false;
        }
        for (String candidate : commaSeparatedValues.split(",")) {
            if (candidate.trim().equalsIgnoreCase(value.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when the origin airport is in Bangladesh — by country code or {@link BangladeshAirport} IATA code.
     */
    boolean isBangladeshOrigin(Airport airport) {
        if (airport == null) {
            return false;
        }
        if ("BD".equalsIgnoreCase(airport.getCountryCode())) {
            return true;
        }
        return BangladeshAirport.isBangladeshAirport(airport.getAirportCode());
    }

    private double calculateAmount(double baseAmount, String type, Double value) {
        if (value == null) return 0.0;
        if ("PERCENTAGE".equalsIgnoreCase(type) || "%".equals(type)) {
            return baseAmount * (value / 100.0);
        } else {
            return value;
        }
    }


    @Async
    public void logMarkupApplicationAsync(String sessionId, MarkupRule rule, double original, double markup, double finalPrice, MarkupContext context) {
        try {
            MarkupLog markupLog = new MarkupLog();
            markupLog.setSessionId(sessionId);
            markupLog.setRuleApplied("Rule ID: " + rule.getId());
            markupLog.setOriginalPrice(original);
            markupLog.setMarkupAmount(markup);
            markupLog.setFinalPrice(finalPrice);
            markupLog.setUserType(context.getUserType());
            markupLog.setUserId(context.getUserId());
            markupLog.setBusinessId(context.getBusinessId());
            markupLogRepository.save(markupLog);
        } catch (Exception e) {
            log.warn("[Session: {}] Failed to log markup application: {}", sessionId, e.getMessage());
        }
    }

    // Business Binding Management Methods

    /**
     * Bind multiple markup plans to multiple businesses at once.
     * Each (plan, business) pair is handled independently: reactivated if already exists, created otherwise.
     */
    @Transactional
    public List<MarkupPlanBusinessResponse> bindPlansToBusinesses(List<Long> businessIds, List<Long> markupPlanIds) {
        if (businessIds == null || businessIds.isEmpty()) {
            throw new IllegalArgumentException("Business IDs cannot be null or empty");
        }
        if (markupPlanIds == null || markupPlanIds.isEmpty()) {
            throw new IllegalArgumentException("Markup plan IDs cannot be null or empty");
        }

        List<MarkupPlan> plans = markupPlanRepository.findAllById(markupPlanIds);
        if (plans.size() != markupPlanIds.size()) {
            throw new ResourceNotFoundException("Some MarkupPlan IDs were");
        }

        List<MarkupPlanBusiness> toSave = new ArrayList<>();

        for (Long businessId : businessIds) {
            for (MarkupPlan plan : plans) {
                MarkupPlanBusiness existing = markupPlanBusinessRepository
                        .findByMarkupPlanIdAndBusinessId(plan.getId(), businessId)
                        .orElse(null);

                if (existing != null) {
                    existing.setIsActive(true);
                    toSave.add(existing);
                } else {
                    MarkupPlanBusiness binding = new MarkupPlanBusiness();
                    binding.setMarkupPlan(plan);
                    binding.setBusinessId(businessId);
                    binding.setIsActive(true);
                    toSave.add(binding);
                }
            }
        }

        List<MarkupPlanBusiness> savedBindings = markupPlanBusinessRepository.saveAll(toSave);

        // Invalidate cache for all affected businesses
        for (Long businessId : businessIds) {
            rulesCache.invalidate("AGENT_" + businessId);
        }

        log.info("Bulk-bound {} markup plans to {} businesses", markupPlanIds.size(), businessIds.size());

        return savedBindings.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<MarkupPlanBusinessResponse> bindPlansToBusiness(Long businessId, List<Long> markupPlanIds) {
        if (markupPlanIds == null || markupPlanIds.isEmpty()) {
            throw new IllegalArgumentException("Markup plan IDs cannot be null or empty");
        }

        // Fetch all plans in one query
        List<MarkupPlan> plans = markupPlanRepository.findAllById(markupPlanIds);
        if (plans.size() != markupPlanIds.size()) {
            throw new ResourceNotFoundException("Some MarkupPlan IDs were");
        }

        List<MarkupPlanBusinessResponse> responses = new ArrayList<>();
        List<MarkupPlanBusiness> toSave = new ArrayList<>();

        for (MarkupPlan plan : plans) {
            // Check if binding already exists
            MarkupPlanBusiness existing = markupPlanBusinessRepository
                    .findByMarkupPlanIdAndBusinessId(plan.getId(), businessId)
                    .orElse(null);

            if (existing != null) {
                // Reactivate if exists
                existing.setIsActive(true);
                toSave.add(existing);
            } else {
                // Create new binding
                MarkupPlanBusiness binding = new MarkupPlanBusiness();
                binding.setMarkupPlan(plan);
                binding.setBusinessId(businessId);
                binding.setIsActive(true);
                toSave.add(binding);
            }
        }

        // Batch save all bindings
        List<MarkupPlanBusiness> savedBindings = markupPlanBusinessRepository.saveAll(toSave);
        for (MarkupPlanBusiness binding : savedBindings) {
            responses.add(toResponse(binding));
        }

        // Invalidate cache for this business
        rulesCache.invalidate("AGENT_" + businessId);

        log.info("Bound {} markup plans to business {}", markupPlanIds.size(), businessId);
        return responses;
    }

    /**
     * List all plan-business bindings with pagination.
     * Pass activeOnly=true to return only active bindings.
     */
    public Page<MarkupPlanBusinessResponse> getAllBindings(Boolean activeOnly, Pageable pageable) {
        Page<MarkupPlanBusiness> page;
        if (activeOnly == null) {
            page = markupPlanBusinessRepository.findAll(pageable);
        } else if (activeOnly) {
            page = markupPlanBusinessRepository.findAllByIsActiveTrue(pageable);
        } else {
            page = markupPlanBusinessRepository.findAllByIsActiveFalse(pageable);
        }
        return page.map(this::toResponse);
    }

    public List<MarkupPlanBusinessResponse> getBusinessesByPlan(Long planId) {
        return markupPlanBusinessRepository.findAllByMarkupPlanId(planId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<MarkupPlanBusinessResponse> getPlansByBusiness(Long businessId) {
        return markupPlanBusinessRepository.findByBusinessIdAndIsActiveTrue(businessId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void unbindBusinessFromPlan(Long planId, Long businessId) {
        MarkupPlanBusiness binding = markupPlanBusinessRepository
                .findByMarkupPlanIdAndBusinessId(planId, businessId)
                .orElseThrow(() -> ServiceExceptions.business("No binding found for plan " + planId + " and business " + businessId));

        // Soft delete by deactivating
        binding.setIsActive(false);
        markupPlanBusinessRepository.save(binding);

        // Invalidate cache for this business
        rulesCache.invalidate("AGENT_" + businessId);

        log.info("Unbound business {} from markup plan {}", businessId, planId);
    }

    private MarkupPlanBusinessResponse toResponse(MarkupPlanBusiness binding) {
        String planTitle = binding.getMarkupPlan() != null ? binding.getMarkupPlan().getTitle() : null;
        String businessName = businessRepository.findById(binding.getBusinessId())
                .map(BusinessEntity::getCompanyName)
                .orElse(null);
        return new MarkupPlanBusinessResponse(
                binding.getId(),
                binding.getMarkupPlan() != null ? binding.getMarkupPlan().getId() : null,
                planTitle,
                binding.getBusinessId(),
                businessName,
                binding.getIsActive(),
                timestampMapper.toRequestUserTime(binding.getCreatedAt(), binding.getCreatedTimeOffset()));
    }
}
