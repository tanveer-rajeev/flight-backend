package com.aerionsoft.application.service.breakingnews;

import com.aerionsoft.application.dto.breakingnews.BreakingNewsRequest;
import com.aerionsoft.application.dto.breakingnews.BreakingNewsResponse;
import com.aerionsoft.application.entity.BreakingNews;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.enums.breakingnews.BreakingNewsTarget;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.repository.breakingnews.BreakingNewsRepository;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.service.user.CustomUserDetails;
import com.aerionsoft.application.util.TimestampMapper;
import com.aerionsoft.application.util.UserDateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BreakingNewsServiceImpl implements BreakingNewsService {

    private final BreakingNewsRepository breakingNewsRepository;
    private final AdminUserRepository adminUserRepository;
    private final TimestampMapper timestampMapper;

    // ── Admin operations ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public BreakingNewsResponse create(BreakingNewsRequest request) {
        log.info("Creating breaking news: {}", request.getTitle());

        BreakingNews news = BreakingNews.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .target(request.getTarget())
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .active(request.isActive())
                .startsAt(request.getStartsAt())
                .expiresAt(request.getExpiresAt())
                .linkUrl(request.getLinkUrl())
                .imageUrl(request.getImageUrl())
                .createdBy(getCurrentAdminId())
                .build();

        breakingNewsRepository.save(news);
        log.info("Breaking news created with ID: {}", news.getId());
        return toResponse(news);
    }

    @Override
    @Transactional
    public BreakingNewsResponse update(Long id, BreakingNewsRequest request) {
        log.info("Updating breaking news ID: {}", id);

        BreakingNews news = findOrThrow(id);
        news.setTitle(request.getTitle());
        news.setContent(request.getContent());
        news.setTarget(request.getTarget());
        news.setPriority(request.getPriority() != null ? request.getPriority() : news.getPriority());
        news.setActive(request.isActive());
        news.setStartsAt(request.getStartsAt());
        news.setExpiresAt(request.getExpiresAt());
        news.setLinkUrl(request.getLinkUrl());

        breakingNewsRepository.save(news);
        log.info("Breaking news ID {} updated", id);
        return toResponse(news);
    }

    @Override
    @Transactional
    public BreakingNewsResponse toggleActive(Long id) {
        BreakingNews news = findOrThrow(id);
        news.setActive(!news.isActive());
        breakingNewsRepository.save(news);
        log.info("Breaking news ID {} active toggled to {}", id, news.isActive());
        return toResponse(news);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        findOrThrow(id); // ensure it exists
        breakingNewsRepository.deleteById(id);
        log.info("Breaking news ID {} deleted", id);
    }

    @Override
    public BreakingNewsResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public Page<BreakingNewsResponse> getAll(Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "priority", "createdAt"));
        Page<BreakingNews> result = (active != null)
                ? breakingNewsRepository.findByActiveOrderByPriorityDescCreatedAtDesc(active, pageable)
                : breakingNewsRepository.findAllByOrderByPriorityDescCreatedAtDesc(pageable);
        return result.map(this::toResponse);
    }

    @Override
    public List<BreakingNewsResponse> getAll() {
        return breakingNewsRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Client operations ──────────────────────────────────────────────────────

    @Override
    public List<BreakingNewsResponse> getActiveForTarget(BreakingNewsTarget target) {
        return breakingNewsRepository
                .findActiveForTarget(target, UserDateTimeUtil.now())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<BreakingNewsResponse> getActiveForTargetPaged(BreakingNewsTarget target, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return breakingNewsRepository
                .findActiveForTargetPaged(target, UserDateTimeUtil.now(), pageable)
                .map(this::toResponse);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private BreakingNews findOrThrow(Long id) {
        return breakingNewsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Breaking news", id));
    }

    private Long getCurrentAdminId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
                return details.getId();
            }
        } catch (Exception e) {
            log.warn("Could not resolve current admin ID", e);
        }
        return null;
    }

    private BreakingNewsResponse toResponse(BreakingNews news) {
        String createdByName = null;
        if (news.getCreatedBy() != null) {
            createdByName = adminUserRepository.findById(news.getCreatedBy())
                    .map(AdminUser::getFullName)
                    .orElse(null);
        }

        return BreakingNewsResponse.builder()
                .id(news.getId())
                .title(news.getTitle())
                .content(news.getContent())
                .target(news.getTarget())
                .priority(news.getPriority())
                .active(news.isActive())
                .startsAt(news.getStartsAt())
                .expiresAt(news.getExpiresAt())
                .linkUrl(news.getLinkUrl())
                .imageUrl(news.getImageUrl())
                .createdBy(news.getCreatedBy())
                .createdByName(createdByName)
                .createdAt(timestampMapper.toRequestUserTime(news.getCreatedAt(), news.getCreatedTimeOffset()))
                .updatedAt(timestampMapper.toRequestUserTime(news.getUpdatedAt(), news.getUpdatedTimeOffset() != null ? news.getUpdatedTimeOffset() : news.getCreatedTimeOffset()))
                .build();
    }
}

