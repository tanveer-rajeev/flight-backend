package com.aerionsoft.application.repository.breakingnews;

import com.aerionsoft.application.entity.BreakingNews;
import com.aerionsoft.application.enums.breakingnews.BreakingNewsTarget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BreakingNewsRepository extends JpaRepository<BreakingNews, Long> {

    /** Admin: paginated list, optionally filtered by active status */
    Page<BreakingNews> findAllByOrderByPriorityDescCreatedAtDesc(Pageable pageable);

    Page<BreakingNews> findByActiveOrderByPriorityDescCreatedAtDesc(boolean active, Pageable pageable);

    /**
     * Active news visible to a given target audience right now.
     * Returns items whose target is ALL, OFFER, PROMOTION, or matches the requested target,
     * and which are within their active date window.
     */
    @Query("""
            SELECT b FROM BreakingNews b
            WHERE b.active = true
              AND (b.target = 'ALL' OR b.target = :target OR b.target IN ('OFFER', 'PROMOTION'))
              AND (b.startsAt IS NULL OR b.startsAt <= :now)
              AND (b.expiresAt IS NULL OR b.expiresAt >= :now)
            ORDER BY b.priority DESC, b.createdAt DESC
            """)
    List<BreakingNews> findActiveForTarget(@Param("target") BreakingNewsTarget target,
                                           @Param("now") LocalDateTime now);

    /** Same as above but paginated */
    @Query("""
            SELECT b FROM BreakingNews b
            WHERE b.active = true
              AND (b.target = 'ALL' OR b.target = :target OR b.target IN ('OFFER', 'PROMOTION'))
              AND (b.startsAt IS NULL OR b.startsAt <= :now)
              AND (b.expiresAt IS NULL OR b.expiresAt >= :now)
            ORDER BY b.priority DESC, b.createdAt DESC
            """)
    Page<BreakingNews> findActiveForTargetPaged(@Param("target") BreakingNewsTarget target,
                                                @Param("now") LocalDateTime now,
                                                Pageable pageable);
}

