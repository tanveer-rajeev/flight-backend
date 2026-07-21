package com.aerionsoft.application.repository.tour;

import com.aerionsoft.application.entity.tour.TourFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TourFavoriteRepository extends JpaRepository<TourFavorite, Long> {

    boolean existsByUserIdAndTourPackageId(Long userId, Long tourPackageId);

    Optional<TourFavorite> findByUserIdAndTourPackageId(Long userId, Long tourPackageId);

    List<TourFavorite> findByUserId(Long userId);

    void deleteByUserIdAndTourPackageId(Long userId, Long tourPackageId);
}
