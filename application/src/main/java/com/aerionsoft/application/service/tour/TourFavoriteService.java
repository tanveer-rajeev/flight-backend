package com.aerionsoft.application.service.tour;

import com.aerionsoft.application.dto.tour.TourFavoriteResponse;

import java.util.List;

public interface TourFavoriteService {

    void addFavorite(Long userId, Long tourPackageId);

    void removeFavorite(Long userId, Long tourPackageId);

    List<TourFavoriteResponse> getUserFavorites(Long userId);

    boolean isFavorite(Long userId, Long tourPackageId);
}
