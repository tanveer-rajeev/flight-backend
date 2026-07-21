package com.aerionsoft.application.service.tour;

import com.aerionsoft.application.dto.tour.TourCategoryRequest;
import com.aerionsoft.application.dto.tour.TourCategoryResponse;
import com.aerionsoft.application.entity.tour.TourCategory;

import java.util.List;

public interface TourCategoryService {

    TourCategoryResponse create(TourCategoryRequest request);

    TourCategoryResponse update(Long id, TourCategoryRequest request);

    TourCategoryResponse getById(Long id);

    List<TourCategoryResponse> getAll(Boolean activeOnly);

    List<TourCategoryResponse> getAllWithTours(Boolean activeOnly);

    void delete(Long id);

    TourCategory resolveActiveCategory(Long categoryId);
}
