package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.breakingnews.BreakingNewsRequest;
import com.aerionsoft.application.dto.breakingnews.BreakingNewsResponse;
import com.aerionsoft.application.service.breakingnews.BreakingNewsService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/admin/breaking-news")
@RequiredArgsConstructor
public class BreakingNewsAdminController {

    private final BreakingNewsService breakingNewsService;

    /** Create a new breaking-news item */
    @PostMapping
    public ResponseEntity<BaseResponse<BreakingNewsResponse>> create(
            @Valid @RequestBody BreakingNewsRequest request) {
        BreakingNewsResponse response = breakingNewsService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Breaking news created successfully", response));
    }

    /** Update an existing item */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<BreakingNewsResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody BreakingNewsRequest request) {
        return ResponseEntity.ok(BaseResponse.ok("Breaking news updated successfully", breakingNewsService.update(id, request)));
    }

    /** Toggle active / inactive */
    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<BaseResponse<BreakingNewsResponse>> toggleActive(@PathVariable Long id) {
        BreakingNewsResponse response = breakingNewsService.toggleActive(id);
        String msg = response.isActive() ? "Breaking news activated" : "Breaking news deactivated";
        return ResponseEntity.ok(BaseResponse.ok(msg, response));
    }

    /** Delete */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable Long id) {
        breakingNewsService.delete(id);
        return ResponseEntity.ok(BaseResponse.ok("Breaking news deleted successfully"));
    }

    /** Get single item */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<BreakingNewsResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.ok(breakingNewsService.getById(id)));
    }

    /**
     * List all items (paginated).
     * Optional ?active=true|false filter.
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<BreakingNewsResponse>>> getAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(BaseResponse.ok(breakingNewsService.getAll(active, page, size)));
    }
}
