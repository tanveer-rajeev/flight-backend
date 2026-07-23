package com.aerionsoft.application.service.audit;

import com.aerionsoft.application.entity.audit.ActivityLog;
import com.aerionsoft.application.enums.audit.ActivityEventCategory;
import com.aerionsoft.application.enums.audit.ActivityEventType;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ActivityFeedFilter {

    static final Set<ActivityEventCategory> DEFAULT_OPS_CATEGORIES = EnumSet.of(
            ActivityEventCategory.BOOKING,
            ActivityEventCategory.WALLET,
            ActivityEventCategory.TICKET_ACTION);

    private ActivityFeedFilter() {
    }

    static Set<ActivityEventCategory> resolveCategories(List<ActivityEventCategory> requested) {
        if (requested == null || requested.isEmpty()) {
            return DEFAULT_OPS_CATEGORIES;
        }
        return EnumSet.copyOf(requested);
    }

    public static boolean isFeedEligible(ActivityLog log) {
        if (log == null || log.getEventCategory() == null) {
            return false;
        }
        if (log.getEventType() == ActivityEventType.ADMIN_ACTION) {
            return false;
        }
        return DEFAULT_OPS_CATEGORIES.contains(log.getEventCategory());
    }
}
