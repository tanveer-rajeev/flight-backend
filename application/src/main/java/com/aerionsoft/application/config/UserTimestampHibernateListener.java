package com.aerionsoft.application.config;

import java.util.Set;

import com.aerionsoft.application.util.UserDateTimeUtil;

import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

/**
 * Sets user-local wall-clock timestamps and offsets on insert/update for any mapped property.
 */
@Component
public class UserTimestampHibernateListener implements PreInsertEventListener, PreUpdateEventListener {

    private static final Set<String> CREATED_AT_NAMES = Set.of(
            "createdAt", "createAt", "submittedAt", "uploadedAt", "searchedAt", "sentAt", "loginAt"
    );
    private static final Set<String> UPDATED_AT_NAMES = Set.of("updatedAt", "updateAt");

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        apply(event.getPersister(), event.getState(), true);
        return false;
    }

    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        apply(event.getPersister(), event.getState(), false);
        return false;
    }

    private void apply(EntityPersister persister, Object[] state, boolean insert) {
        String[] names = persister.getPropertyNames();
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            if (insert && CREATED_AT_NAMES.contains(name) && state[i] == null) {
                state[i] = UserDateTimeUtil.now();
            }
            if (insert && "createdTimeOffset".equals(name) && state[i] == null) {
                state[i] = UserDateTimeUtil.currentOffset();
            }
            if (UPDATED_AT_NAMES.contains(name)) {
                state[i] = UserDateTimeUtil.now();
            }
            if ("updatedTimeOffset".equals(name)) {
                state[i] = UserDateTimeUtil.currentOffset();
            }
        }
    }
}
