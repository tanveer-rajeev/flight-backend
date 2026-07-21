package com.aerionsoft.application.service.tour;

import com.aerionsoft.application.dto.tour.TourCategoryRequest;
import com.aerionsoft.application.dto.tour.TourCategoryResponse;
import com.aerionsoft.application.entity.tour.TourCategory;
import com.aerionsoft.application.entity.tour.TourPackage;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.repository.tour.TourCategoryRepository;
import com.aerionsoft.application.repository.tour.TourPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourCategoryServiceImpl implements TourCategoryService {

    private final TourCategoryRepository tourCategoryRepository;
    private final TourPackageRepository tourPackageRepository;

    @Override
    @Transactional
    public TourCategoryResponse create(TourCategoryRequest request) {
        if (tourCategoryRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Tour category with this name already exists");
        }
        TourCategory category = new TourCategory();
        mapRequestToEntity(request, category);
        return mapEntityToResponse(tourCategoryRepository.save(category), false);
    }

    @Override
    @Transactional
    public TourCategoryResponse update(Long id, TourCategoryRequest request) {
        TourCategory category = tourCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour category", id));
        mapRequestToEntity(request, category);
        return mapEntityToResponse(tourCategoryRepository.save(category), false);
    }

    @Override
    @Transactional(readOnly = true)
    public TourCategoryResponse getById(Long id) {
        TourCategory category = tourCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour category", id));
        return mapEntityToResponse(category, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourCategoryResponse> getAll(Boolean activeOnly) {
        List<TourCategory> categories = Boolean.TRUE.equals(activeOnly)
                ? tourCategoryRepository.findByIsActiveTrueOrderByNameAsc()
                : tourCategoryRepository.findAll();
        return categories.stream()
                .map(c -> mapEntityToResponse(c, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourCategoryResponse> getAllWithTours(Boolean activeOnly) {
        List<TourCategory> categories = Boolean.TRUE.equals(activeOnly)
                ? tourCategoryRepository.findByIsActiveTrueOrderByNameAsc()
                : tourCategoryRepository.findAll();
        return categories.stream()
                .map(c -> mapEntityToResponse(c, true))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TourCategory category = tourCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour category", id));
        List<TourPackage> linked = tourPackageRepository.findByCategoriesContaining(category);
        if (!linked.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Cannot delete category that is assigned to tour packages");
        }
        tourCategoryRepository.deleteById(id);
    }

    @Override
    public TourCategory resolveActiveCategory(Long categoryId) {
        TourCategory category = tourCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour category", categoryId));
        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tour category is inactive");
        }
        return category;
    }

    private void mapRequestToEntity(TourCategoryRequest request, TourCategory entity) {
        if (request.getName() != null) {
            entity.setName(request.getName().trim());
        }
        entity.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
    }

    TourCategoryResponse mapEntityToResponse(TourCategory entity, boolean includeTours) {
        TourCategoryResponse response = new TourCategoryResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        if (includeTours) {
            List<TourPackage> tours = tourPackageRepository.findByCategoriesContaining(entity);
            response.setTours(tours.stream()
                    .map(tp -> new TourCategoryResponse.TourPackageSummaryResponse(
                            tp.getId(), tp.getTitle(),
                            tp.getDestinationCity(), tp.getDestinationCountry(),
                            tp.getStatus().name()))
                    .collect(Collectors.toList()));
        }
        return response;
    }
}
