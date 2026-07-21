package com.aerionsoft.application.controller.tour;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.tour.TourFavoriteResponse;
import com.aerionsoft.application.service.tour.TourFavoriteService;
import com.aerionsoft.application.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/user/tour-favorites")
@RequiredArgsConstructor
public class TourFavoriteController {

    private final TourFavoriteService tourFavoriteService;
    private final UserService userService;

    @PostMapping("/{tourPackageId}")
    public ResponseEntity<BaseResponse<Void>> addFavorite(
            @PathVariable Long tourPackageId,
            Authentication authentication) {
        Long userId = userService.getUserIdByEmail(authentication.getName());
        tourFavoriteService.addFavorite(userId, tourPackageId);
        return ResponseEntity.ok(BaseResponse.ok((Void) null, "Tour package added to favorites"));
    }

    @DeleteMapping("/{tourPackageId}")
    public ResponseEntity<BaseResponse<Void>> removeFavorite(
            @PathVariable Long tourPackageId,
            Authentication authentication) {
        Long userId = userService.getUserIdByEmail(authentication.getName());
        tourFavoriteService.removeFavorite(userId, tourPackageId);
        return ResponseEntity.ok(BaseResponse.ok((Void) null, "Tour package removed from favorites"));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<List<TourFavoriteResponse>>> getUserFavorites(
            Authentication authentication) {
        Long userId = userService.getUserIdByEmail(authentication.getName());
        List<TourFavoriteResponse> response = tourFavoriteService.getUserFavorites(userId);
        return ResponseEntity.ok(BaseResponse.ok(response, "User favorites retrieved successfully"));
    }

    @GetMapping("/{tourPackageId}/status")
    public ResponseEntity<BaseResponse<Boolean>> isFavorite(
            @PathVariable Long tourPackageId,
            Authentication authentication) {
        Long userId = userService.getUserIdByEmail(authentication.getName());
        boolean favorite = tourFavoriteService.isFavorite(userId, tourPackageId);
        return ResponseEntity.ok(BaseResponse.ok(favorite, "Favorite status retrieved successfully"));
    }
}
