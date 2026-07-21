package com.aerionsoft.application.service.tour;

import com.aerionsoft.application.dto.tour.TourFavoriteResponse;
import com.aerionsoft.application.entity.tour.TourFavorite;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.repository.cms.MediaRepository;
import com.aerionsoft.application.repository.tour.TourFavoriteRepository;
import com.aerionsoft.application.repository.tour.TourPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourFavoriteServiceImpl implements TourFavoriteService {

    private final TourFavoriteRepository tourFavoriteRepository;
    private final TourPackageRepository tourPackageRepository;
    private final MediaRepository mediaRepository;

    @Override
    @Transactional
    public void addFavorite(Long userId, Long tourPackageId) {
        if (!tourPackageRepository.existsById(tourPackageId)) {
            throw new ResourceNotFoundException("Tour package", tourPackageId);
        }
        if (tourFavoriteRepository.existsByUserIdAndTourPackageId(userId, tourPackageId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Tour package is already in favorites");
        }
        TourFavorite favorite = new TourFavorite();
        favorite.setUserId(userId);
        favorite.setTourPackageId(tourPackageId);
        tourFavoriteRepository.save(favorite);
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Long tourPackageId) {
        if (!tourFavoriteRepository.existsByUserIdAndTourPackageId(userId, tourPackageId)) {
            throw new ResourceNotFoundException("Favorite tour package", tourPackageId);
        }
        tourFavoriteRepository.deleteByUserIdAndTourPackageId(userId, tourPackageId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourFavoriteResponse> getUserFavorites(Long userId) {
        return tourFavoriteRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isFavorite(Long userId, Long tourPackageId) {
        return tourFavoriteRepository.existsByUserIdAndTourPackageId(userId, tourPackageId);
    }

    private TourFavoriteResponse mapToResponse(TourFavorite favorite) {
        TourFavoriteResponse response = new TourFavoriteResponse();
        response.setId(favorite.getId());
        response.setTourPackageId(favorite.getTourPackageId());
        response.setFavoritedAt(favorite.getCreatedAt());
        tourPackageRepository.findById(favorite.getTourPackageId()).ifPresent(tp -> {
            response.setTourTitle(tp.getTitle());
            response.setDestinationCity(tp.getDestinationCity());
            response.setDestinationCountry(tp.getDestinationCountry());
            mediaRepository.findByPackageIdOrderBySortOrder(tp.getId()).stream()
                    .findFirst()
                    .ifPresent(m -> response.setThumbnailUrl(m.getFileUrl()));
        });
        return response;
    }
}
