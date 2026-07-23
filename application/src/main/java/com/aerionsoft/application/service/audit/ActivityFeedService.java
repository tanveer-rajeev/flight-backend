package com.aerionsoft.application.service.audit;

import com.aerionsoft.application.dto.audit.ActivityFeedEvent;
import com.aerionsoft.application.entity.audit.ActivityLog;
import com.aerionsoft.application.enums.audit.ActivityEventCategory;
import com.aerionsoft.application.repository.audit.ActivityLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class ActivityFeedService {

    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_LIMIT = 50;

    private final ActivityLogRepository activityLogRepository;
    private final ActivityFeedMapper activityFeedMapper;

    public ActivityFeedService(
            ActivityLogRepository activityLogRepository,
            ActivityFeedMapper activityFeedMapper) {
        this.activityLogRepository = activityLogRepository;
        this.activityFeedMapper = activityFeedMapper;
    }

    @Transactional(readOnly = true)
    public List<ActivityFeedEvent> getFeed(Long sinceId, List<ActivityEventCategory> categories, int limit) {
        int pageSize = normalizeLimit(limit);
        Set<ActivityEventCategory> resolvedCategories = ActivityFeedFilter.resolveCategories(categories);

        List<ActivityLog> logs;
        if (sinceId != null && sinceId > 0) {
            logs = activityLogRepository.findByIdGreaterThanAndEventCategoryInOrderByIdAsc(
                    sinceId,
                    resolvedCategories,
                    PageRequest.of(0, pageSize));
        } else {
            logs = new ArrayList<>(activityLogRepository.findByEventCategoryInOrderByIdDesc(
                    resolvedCategories,
                    PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "id"))));
            logs.sort(Comparator.comparing(ActivityLog::getId));
        }

        logs = logs.stream().filter(ActivityFeedFilter::isFeedEligible).toList();
        return activityFeedMapper.toFeedEvents(logs);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
