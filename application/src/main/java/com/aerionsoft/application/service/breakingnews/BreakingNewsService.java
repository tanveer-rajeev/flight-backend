package com.aerionsoft.application.service.breakingnews;

import com.aerionsoft.application.dto.breakingnews.BreakingNewsRequest;
import com.aerionsoft.application.dto.breakingnews.BreakingNewsResponse;
import com.aerionsoft.application.enums.breakingnews.BreakingNewsTarget;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BreakingNewsService {

    // ── Admin ──────────────────────────────────────────────────────────────────

    /** Create a new breaking-news item */
    BreakingNewsResponse create(BreakingNewsRequest request);

    /** Update an existing breaking-news item */
    BreakingNewsResponse update(Long id, BreakingNewsRequest request);

    /** Soft-toggle active flag */
    BreakingNewsResponse toggleActive(Long id);

    /** Hard delete */
    void delete(Long id);

    /** Get single item by id */
    BreakingNewsResponse getById(Long id);

    /** Paginated list for admin (optional active filter) */
    Page<BreakingNewsResponse> getAll(Boolean active, int page, int size);

    List<BreakingNewsResponse> getAll();

    // ── Client (user / agency) ─────────────────────────────────────────────────

    /**
     * Returns all currently-active news visible to the given audience.
     * Respects startsAt / expiresAt window automatically.
     */
    List<BreakingNewsResponse> getActiveForTarget(BreakingNewsTarget target);

    /** Paginated variant */
    Page<BreakingNewsResponse> getActiveForTargetPaged(BreakingNewsTarget target, int page, int size);
}

