package com.aerionsoft.application.entity.listener;

import com.aerionsoft.application.entity.HasCreatedUserTimestamp;
import com.aerionsoft.application.entity.HasUpdatedUserTimestamp;
import com.aerionsoft.application.util.UserDateTimeUtil;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class UserTimestampListener {

    @PrePersist
    public void onPrePersist(Object entity) {
        applyCreated(entity);
        applyUpdated(entity);
    }

    @PreUpdate
    public void onPreUpdate(Object entity) {
        applyUpdated(entity);
    }

    private void applyCreated(Object entity) {
        if (!(entity instanceof HasCreatedUserTimestamp created)) {
            return;
        }
        if (created.getCreatedAt() == null) {
            created.setCreatedAt(UserDateTimeUtil.now());
        }
        if (created.getCreatedTimeOffset() == null) {
            created.setCreatedTimeOffset(UserDateTimeUtil.currentOffset());
        }
    }

    private void applyUpdated(Object entity) {
        if (!(entity instanceof HasUpdatedUserTimestamp updated)) {
            return;
        }
        updated.setUpdatedAt(UserDateTimeUtil.now());
        updated.setUpdatedTimeOffset(UserDateTimeUtil.currentOffset());
    }
}
