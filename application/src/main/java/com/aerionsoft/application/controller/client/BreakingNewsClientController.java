package com.aerionsoft.application.controller.client;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.breakingnews.BreakingNewsResponse;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.enums.breakingnews.BreakingNewsTarget;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.service.breakingnews.BreakingNewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/breaking-news")
@RequiredArgsConstructor
public class BreakingNewsClientController extends BaseController {

    private final BreakingNewsService breakingNewsService;
    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;


    @GetMapping
    public ResponseEntity<BaseResponse<List<BreakingNewsResponse>>> getAllBreakingNews() {
        List<BreakingNewsResponse> news = breakingNewsService.getAll();
        return ResponseEntity.ok(BaseResponse.ok(news));
    }

    /** Paginated variant */
    @GetMapping("/paged")
    public ResponseEntity<BaseResponse<Page<BreakingNewsResponse>>> getMyBreakingNewsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        BreakingNewsTarget target = resolveTarget();
        Page<BreakingNewsResponse> news = breakingNewsService.getActiveForTargetPaged(target, page, size);
        return ResponseEntity.ok(BaseResponse.ok(news));
    }

    /** Single item detail */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<BreakingNewsResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.ok(breakingNewsService.getById(id)));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    /**
     * Determines whether the calling user is an AGENCY member or a plain USER
     * so the correct breaking-news feed is returned.
     */
    private BreakingNewsTarget resolveTarget() {
        Long userId = getUserIdFromAuthentication();

        if (userId == null) {
            return BreakingNewsTarget.USER;
        }

        return userRepository.findById(userId).filter(User::isAgency).map(user -> BreakingNewsTarget.AGENCY).orElse(BreakingNewsTarget.USER);
    }
}
