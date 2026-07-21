package com.aerionsoft.application.service.tour;

import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.enums.tour.TourFlag;
import com.aerionsoft.application.dto.tour.TourCategoryResponse;
import com.aerionsoft.application.dto.tour.TourPackageListItemResponse;
import com.aerionsoft.application.dto.tour.TourPackageRequest;
import com.aerionsoft.application.dto.tour.TourPackageResponse;
import com.aerionsoft.application.dto.tour.TourPackageSearchResponse;
import com.aerionsoft.application.dto.tour.TourPackageTypeResponse;
import com.aerionsoft.application.entity.Accommodation;
import com.aerionsoft.application.entity.tour.Itinerary;
import com.aerionsoft.application.entity.tour.Meal;
import com.aerionsoft.application.entity.Media;
import com.aerionsoft.application.entity.PackageItem;
import com.aerionsoft.application.entity.tour.Pricing;
import com.aerionsoft.application.entity.tour.TourCategory;
import com.aerionsoft.application.entity.tour.TourPackage;
import com.aerionsoft.application.entity.tour.Transport;
import com.aerionsoft.application.repository.flight.AccommodationRepository;
import com.aerionsoft.application.repository.flight.MealRepository;
import com.aerionsoft.application.repository.cms.MediaRepository;
import com.aerionsoft.application.repository.cms.PackageItemRepository;
import com.aerionsoft.application.repository.group.ItineraryRepository;
import com.aerionsoft.application.repository.group.PricingRepository;
import com.aerionsoft.application.repository.tour.TourCategoryRepository;
import com.aerionsoft.application.repository.tour.TourPackageRepository;
import com.aerionsoft.application.repository.tour.TourPackageSpec;
import com.aerionsoft.application.repository.tour.TransportRepository;
import com.aerionsoft.application.util.TimestampMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourPackageServiceImpl implements TourPackageService {

    private static final int MIN_SEARCH_KEYWORD_LENGTH = 2;
    private static final int MAX_SEARCH_KEYWORD_LENGTH = 100;

    private final TourPackageRepository tourPackageRepository;
    private final MediaRepository mediaRepository;
    private final PricingRepository pricingRepository;
    private final PackageItemRepository packageItemRepository;
    private final ItineraryRepository itineraryRepository;
    private final MealRepository mealRepository;
    private final AccommodationRepository accommodationRepository;
    private final TransportRepository transportRepository;
    private final TimestampMapper timestampMapper;
    private final TourPackageTypeService tourPackageTypeService;
    private final TourCategoryRepository tourCategoryRepository;

    @Override
    @Transactional
    public TourPackageResponse createTourPackage(TourPackageRequest request, Long createdBy) {
        TourPackage tourPackage = new TourPackage();
        mapRequestToEntity(request, tourPackage);
        tourPackage.setCreatedBy(createdBy);

        TourPackage savedPackage = tourPackageRepository.save(tourPackage);

        // Save related entities
        saveRelatedEntities(request, savedPackage.getId());

        return mapEntityToResponse(savedPackage);
    }

    @Override
    @Transactional
    public TourPackageResponse updateTourPackage(Long id, TourPackageRequest request) {
        TourPackage existingPackage = tourPackageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour package", id));

        // Delete existing related entities
        deleteRelatedEntities(id);

        // Update main entity
        mapRequestToEntity(request, existingPackage);
        TourPackage updatedPackage = tourPackageRepository.save(existingPackage);

        // Save new related entities
        saveRelatedEntities(request, id);

        return mapEntityToResponse(updatedPackage);
    }

    @Override
    public TourPackageResponse getTourPackageById(Long id) {
        TourPackage tourPackage = tourPackageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour package", id));
        return mapEntityToResponse(tourPackage);
    }

    @Override
    public Page<TourPackageResponse> getAllTourPackages(Pageable pageable) {
        return tourPackageRepository.findAll(pageable)
                .map(this::mapEntityToResponse);
    }

    @Override
    public Page<TourPackageListItemResponse> getAllTourPackagesList(Pageable pageable) {
        return tourPackageRepository.findAll(pageable)
                .map(this::mapEntityToListItem);
    }

    @Override
    public List<TourPackageResponse> getTourPackagesByStatus(String status) {
        TourPackage.PackageStatus packageStatus = TourPackage.PackageStatus.valueOf(status.toUpperCase());
        return tourPackageRepository.findByStatus(packageStatus)
                .stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TourPackageResponse> getTourPackagesByDestination(String country, String city) {
        List<TourPackage> packages;
        if (city != null && !city.isEmpty()) {
            packages = tourPackageRepository.findByDestinationCityIgnoreCase(city);
        } else {
            packages = tourPackageRepository.findByDestinationCountryIgnoreCase(country);
        }
        return packages.stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TourPackageResponse> getTourPackagesByDateRange(LocalDate startDate, LocalDate endDate) {
        return tourPackageRepository.findByDateRange(startDate, endDate)
                .stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TourPackageSearchResponse searchTourPackages(String keyword, Pageable pageable) {
        return searchTourPackagesWithPopular(keyword, pageable, false);
    }

    @Override
    @Transactional
    public Page<TourPackageResponse> searchPublishedTourPackages(String keyword, Pageable pageable) {
        return searchTourPackages(keyword, pageable, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourPackageResponse> getPopularPublishedTourPackages() {
        return getTopSearchedPackages(true);
    }

    private TourPackageSearchResponse searchTourPackagesWithPopular(String keyword, Pageable pageable, boolean publishedOnly) {
        Page<TourPackageResponse> results = searchTourPackages(keyword, pageable, publishedOnly);
        return new TourPackageSearchResponse(results, getTopSearchedPackages(publishedOnly));
    }

    private Page<TourPackageResponse> searchTourPackages(String keyword, Pageable pageable, boolean publishedOnly) {
        String normalizedKeyword = normalizeSearchKeyword(keyword);

        Specification<TourPackage> spec = TourPackageSpec.matchesKeyword(normalizedKeyword);
        if (publishedOnly) {
            spec = spec.and(TourPackageSpec.hasStatus(TourPackage.PackageStatus.PUBLISHED));
        }

        Pageable sortedPageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "startDate")
        );

        Page<TourPackageResponse> results = tourPackageRepository.findAll(spec, sortedPageable)
                .map(this::mapEntityToResponse);

        List<Long> resultIds = results.getContent().stream()
                .map(TourPackageResponse::getId)
                .toList();
        if (!resultIds.isEmpty()) {
            tourPackageRepository.incrementSearchCount(resultIds);
            results.getContent().forEach(dto ->
                    dto.setSearchCount((dto.getSearchCount() == null ? 0L : dto.getSearchCount()) + 1));
        }

        return results;
    }

    private List<TourPackageResponse> getTopSearchedPackages(boolean publishedOnly) {
        List<TourPackage> topPackages = publishedOnly
                ? tourPackageRepository.findTop3ByStatusAndSearchCountGreaterThanOrderBySearchCountDesc(
                        TourPackage.PackageStatus.PUBLISHED, 0L)
                : tourPackageRepository.findTop3BySearchCountGreaterThanOrderBySearchCountDesc(0L);

        return topPackages.stream()
                .map(TourPackage::getId)
                .map(this::getTourPackageById)
                .toList();
    }

    private String normalizeSearchKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Search keyword is required");
        }

        String normalized = keyword.trim();
        if (normalized.length() < MIN_SEARCH_KEYWORD_LENGTH) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Search keyword must be at least " + MIN_SEARCH_KEYWORD_LENGTH + " characters"
            );
        }
        if (normalized.length() > MAX_SEARCH_KEYWORD_LENGTH) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Search keyword must be at most " + MAX_SEARCH_KEYWORD_LENGTH + " characters"
            );
        }

        return normalized;
    }

    @Override
    @Transactional
    public void deleteTourPackage(Long id) {
        if (!tourPackageRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tour package", id);
        }
        deleteRelatedEntities(id);
        tourPackageRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void changePackageStatus(Long id, String status) {
        TourPackage tourPackage = tourPackageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour package", id));

        tourPackage.setStatus(TourPackage.PackageStatus.valueOf(status.toUpperCase()));
        tourPackageRepository.save(tourPackage);
    }

    private void mapRequestToEntity(TourPackageRequest request, TourPackage entity) {
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setFullDescription(request.getFullDescription());
        entity.setDestinationCity(request.getDestinationCity());
        entity.setDestinationCountry(request.getDestinationCountry());
        entity.setMapLocation(request.getMapLocation());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setFromId(request.getFromId());
        if (request.getTypeId() != null) {
            entity.setType(tourPackageTypeService.resolveActiveType(request.getTypeId()));
        }
        if (request.getStatus() != null) {
            entity.setStatus(TourPackage.PackageStatus.valueOf(request.getStatus().toUpperCase()));
        }
        // Flags
        if (request.getFlags() != null) {
            Set<TourFlag> flags = new HashSet<>();
            for (String f : request.getFlags()) {
                flags.add(TourFlag.valueOf(f.toUpperCase()));
            }
            entity.setFlags(flags);
        }
        // Categories
        if (request.getCategoryIds() != null) {
            Set<TourCategory> categories = new HashSet<>();
            for (Long categoryId : request.getCategoryIds()) {
                TourCategory category = tourCategoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tour category", categoryId));
                categories.add(category);
            }
            entity.setCategories(categories);
        }
    }

    private void saveRelatedEntities(TourPackageRequest request, Long packageId) {
        // Save media
        if (request.getMedia() != null) {
            for (TourPackageRequest.MediaRequest mediaReq : request.getMedia()) {
                Media media = new Media();
                media.setPackageId(packageId);
                media.setFileUrl(mediaReq.getFileUrl());
                media.setFileType(Media.FileType.valueOf(mediaReq.getFileType().toUpperCase()));
                media.setSortOrder(mediaReq.getSortOrder());
                mediaRepository.save(media);
            }
        }

        // Save pricing
        if (request.getPricing() != null) {
            for (TourPackageRequest.PricingRequest pricingReq : request.getPricing()) {
                Pricing pricing = new Pricing();
                pricing.setPackageId(packageId);
                pricing.setCategory(Pricing.PricingCategory.valueOf(pricingReq.getCategory().toUpperCase()));
                pricing.setPrice(pricingReq.getPrice());
                pricing.setCurrency(pricingReq.getCurrency());
                pricingRepository.save(pricing);
            }
        }

        // Save package items
        if (request.getPackageItems() != null) {
            for (TourPackageRequest.PackageItemRequest itemReq : request.getPackageItems()) {
                PackageItem item = new PackageItem();
                item.setPackageId(packageId);
                item.setItemType(PackageItem.ItemType.valueOf(itemReq.getItemType().toUpperCase()));
                item.setItemTitle(itemReq.getItemTitle());
                item.setItemDescription(itemReq.getItemDescription());
                item.setSortOrder(itemReq.getSortOrder());
                packageItemRepository.save(item);
            }
        }

        // Save itineraries
        if (request.getItineraries() != null) {
            for (TourPackageRequest.ItineraryRequest itineraryReq : request.getItineraries()) {
                Itinerary itinerary = new Itinerary();
                itinerary.setPackageId(packageId);
                itinerary.setDayNumber(itineraryReq.getDayNumber());
                itinerary.setImageUrl(itineraryReq.getImageUrl());
                itinerary.setActivity(itineraryReq.getActivity());
                Itinerary savedItinerary = itineraryRepository.save(itinerary);

                // Save meals for this itinerary
                if (itineraryReq.getMeals() != null) {
                    for (TourPackageRequest.MealRequest mealReq : itineraryReq.getMeals()) {
                        Meal meal = new Meal();
                        meal.setItineraryId(savedItinerary.getId());
                        meal.setMealType(Meal.MealType.valueOf(mealReq.getMealType().toUpperCase()));
                        meal.setDescription(mealReq.getDescription());
                        mealRepository.save(meal);
                    }
                }

                // Save accommodations for this itinerary
                if (itineraryReq.getAccommodations() != null) {
                    for (TourPackageRequest.AccommodationRequest accReq : itineraryReq.getAccommodations()) {
                        Accommodation accommodation = new Accommodation();
                        accommodation.setItineraryId(savedItinerary.getId());
                        accommodation.setHotelName(accReq.getHotelName());
                        accommodation.setRoomType(accReq.getRoomType());
                        accommodation.setCheckIn(accReq.getCheckIn());
                        accommodation.setCheckOut(accReq.getCheckOut());
                        accommodation.setNotes(accReq.getNotes());
                        accommodationRepository.save(accommodation);
                    }
                }

                // Save transports for this itinerary
                if (itineraryReq.getTransports() != null) {
                    for (TourPackageRequest.TransportRequest transportReq : itineraryReq.getTransports()) {
                        Transport transport = new Transport();
                        transport.setItineraryId(savedItinerary.getId());
                        transport.setTransportType(Transport.TransportType.valueOf(transportReq.getTransportType().toUpperCase()));
                        transport.setProviderName(transportReq.getProviderName());
                        transport.setDepartureLocation(transportReq.getDepartureLocation());
                        transport.setArrivalLocation(transportReq.getArrivalLocation());
                        transport.setDepartureTime(transportReq.getDepartureTime());
                        transport.setArrivalTime(transportReq.getArrivalTime());
                        transport.setNotes(transportReq.getNotes());
                        transportRepository.save(transport);
                    }
                }
            }
        }
    }

    private void deleteRelatedEntities(Long packageId) {
        // Get all itineraries for this package
        List<Itinerary> itineraries = itineraryRepository.findByPackageIdOrderByDayNumber(packageId);

        // Delete itinerary related entities
        for (Itinerary itinerary : itineraries) {
            mealRepository.deleteByItineraryId(itinerary.getId());
            accommodationRepository.deleteByItineraryId(itinerary.getId());
            transportRepository.deleteByItineraryId(itinerary.getId());
        }

        // Delete package related entities
        itineraryRepository.deleteByPackageId(packageId);
        mediaRepository.deleteByPackageId(packageId);
        pricingRepository.deleteByPackageId(packageId);
        packageItemRepository.deleteByPackageId(packageId);
    }

    private TourPackageResponse mapEntityToResponse(TourPackage entity) {
        TourPackageResponse response = new TourPackageResponse();
        response.setId(entity.getId());
        response.setTitle(entity.getTitle());
        response.setDescription(entity.getDescription());
        response.setFullDescription(entity.getFullDescription());
        response.setDestinationCity(entity.getDestinationCity());
        response.setDestinationCountry(entity.getDestinationCountry());
        response.setMapLocation(entity.getMapLocation());
        response.setStartDate(entity.getStartDate());
        response.setEndDate(entity.getEndDate());
        response.setStatus(entity.getStatus().name());
        response.setCreatedBy(entity.getCreatedBy());
        response.setFromId(entity.getFromId());
        response.setSearchCount(entity.getSearchCount());
        response.setViewCount(entity.getViewCount());

        // Flags
        if (entity.getFlags() != null) {
            response.setFlags(entity.getFlags().stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet()));
        }

        // Categories
        if (entity.getCategories() != null) {
            response.setCategories(entity.getCategories().stream()
                    .map(cat -> {
                        TourCategoryResponse catResp = new TourCategoryResponse();
                        catResp.setId(cat.getId());
                        catResp.setName(cat.getName());
                        catResp.setDescription(cat.getDescription());
                        catResp.setIsActive(cat.getIsActive());
                        return catResp;
                    })
                    .collect(Collectors.toList()));
        }

        if (entity.getType() != null) {
            TourPackageTypeResponse typeResponse = new TourPackageTypeResponse();
            typeResponse.setId(entity.getType().getId());
            typeResponse.setName(entity.getType().getName());
            typeResponse.setDescription(entity.getType().getDescription());
            typeResponse.setIsActive(entity.getType().getIsActive());
            response.setType(typeResponse);
        }
        response.setCreatedAt(timestampMapper.toRequestUserTime(entity.getCreatedAt(), entity.getCreatedTimeOffset()));
        response.setUpdatedAt(timestampMapper.toRequestUserTime(entity.getUpdatedAt(), entity.getUpdatedTimeOffset() != null ? entity.getUpdatedTimeOffset() : entity.getCreatedTimeOffset()));

        // Load and map related entities
        response.setMedia(loadMediaForPackage(entity.getId()));
        response.setPricing(loadPricingForPackage(entity.getId()));
        response.setPackageItems(loadPackageItemsForPackage(entity.getId()));
        response.setItineraries(loadItinerariesForPackage(entity.getId()));

        return response;
    }

    private List<TourPackageResponse.MediaResponse> loadMediaForPackage(Long packageId) {
        return mediaRepository.findByPackageIdOrderBySortOrder(packageId)
                .stream()
                .map(media -> new TourPackageResponse.MediaResponse(
                        media.getId(),
                        media.getFileUrl(),
                        media.getFileType().name(),
                        media.getSortOrder()
                ))
                .collect(Collectors.toList());
    }

    private List<TourPackageResponse.PricingResponse> loadPricingForPackage(Long packageId) {
        return pricingRepository.findByPackageId(packageId)
                .stream()
                .map(pricing -> new TourPackageResponse.PricingResponse(
                        pricing.getId(),
                        pricing.getCategory().name(),
                        pricing.getPrice(),
                        pricing.getCurrency()
                ))
                .collect(Collectors.toList());
    }

    private List<TourPackageResponse.PackageItemResponse> loadPackageItemsForPackage(Long packageId) {
        return packageItemRepository.findByPackageIdOrderBySortOrder(packageId)
                .stream()
                .map(item -> new TourPackageResponse.PackageItemResponse(
                        item.getId(),
                        item.getItemType().name(),
                        item.getItemTitle(),
                        item.getItemDescription(),
                        item.getSortOrder()
                ))
                .collect(Collectors.toList());
    }

    private List<TourPackageResponse.ItineraryResponse> loadItinerariesForPackage(Long packageId) {
        return itineraryRepository.findByPackageIdOrderByDayNumber(packageId)
                .stream()
                .map(itinerary -> {
                    List<TourPackageResponse.MealResponse> meals = mealRepository.findByItineraryId(itinerary.getId())
                            .stream()
                            .map(meal -> new TourPackageResponse.MealResponse(
                                    meal.getId(),
                                    meal.getMealType().name(),
                                    meal.getDescription()
                            ))
                            .collect(Collectors.toList());

                    List<TourPackageResponse.AccommodationResponse> accommodations = accommodationRepository.findByItineraryId(itinerary.getId())
                            .stream()
                            .map(acc -> new TourPackageResponse.AccommodationResponse(
                                    acc.getId(),
                                    acc.getHotelName(),
                                    acc.getRoomType(),
                                    acc.getCheckIn(),
                                    acc.getCheckOut(),
                                    acc.getNotes()
                            ))
                            .collect(Collectors.toList());

                    List<TourPackageResponse.TransportResponse> transports = transportRepository.findByItineraryId(itinerary.getId())
                            .stream()
                            .map(transport -> new TourPackageResponse.TransportResponse(
                                    transport.getId(),
                                    transport.getTransportType().name(),
                                    transport.getProviderName(),
                                    transport.getDepartureLocation(),
                                    transport.getArrivalLocation(),
                                    transport.getDepartureTime(),
                                    transport.getArrivalTime(),
                                    transport.getNotes()
                            ))
                            .collect(Collectors.toList());

                    return new TourPackageResponse.ItineraryResponse(
                            itinerary.getId(),
                            itinerary.getDayNumber(),
                            itinerary.getImageUrl(),
                            itinerary.getActivity(),
                            meals,
                            accommodations,
                            transports
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourPackageResponse> getTourPackagesByFlag(String flag) {
        TourFlag tourFlag = TourFlag.valueOf(flag.toUpperCase());
        return tourPackageRepository.findByFlagsContaining(tourFlag).stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourPackageResponse> getPublishedTourPackagesByFlag(String flag) {
        TourFlag tourFlag = TourFlag.valueOf(flag.toUpperCase());
        return tourPackageRepository.findByStatusAndFlagsContaining(TourPackage.PackageStatus.PUBLISHED, tourFlag)
                .stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void recordTourView(Long id) {
        if (!tourPackageRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tour package", id);
        }
        tourPackageRepository.incrementViewCount(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTourViewCount(Long id) {
        Long count = tourPackageRepository.getViewCount(id);
        if (count == null) {
            throw new ResourceNotFoundException("Tour package", id);
        }
        return count;
    }

    private TourPackageListItemResponse mapEntityToListItem(TourPackage entity) {
        TourPackageListItemResponse response = new TourPackageListItemResponse();
        response.setId(entity.getId());
        response.setTitle(entity.getTitle());
        response.setDestinationCity(entity.getDestinationCity());
        response.setStartDate(entity.getStartDate());
        response.setEndDate(entity.getEndDate());
        
        // Get the first image from media (if available)
        List<Media> mediaList = mediaRepository.findByPackageIdOrderBySortOrder(entity.getId());
        if (mediaList != null && !mediaList.isEmpty()) {
            response.setImage(mediaList.get(0).getFileUrl());
        }
        
        // Load and map pricing
        List<TourPackageListItemResponse.PricingDetail> pricingDetails = loadPricingForPackage(entity.getId())
                .stream()
                .map(pricing -> new TourPackageListItemResponse.PricingDetail(
                        pricing.getCategory(),
                        pricing.getPrice(),
                        pricing.getCurrency()
                ))
                .collect(Collectors.toList());
        
        response.setPricing(pricingDetails);
        return response;
    }
}
