package com.aerionsoft.application.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.stereotype.Component;

@Component
public class HibernateEventListenerRegistrar {

    private final EntityManagerFactory entityManagerFactory;
    private final UserTimestampHibernateListener listener;

    public HibernateEventListenerRegistrar(
            EntityManagerFactory entityManagerFactory,
            UserTimestampHibernateListener listener) {
        this.entityManagerFactory = entityManagerFactory;
        this.listener = listener;
    }

    @PostConstruct
    public void register() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry()
                .getService(EventListenerRegistry.class);
        registry.appendListeners(EventType.PRE_INSERT, listener);
        registry.appendListeners(EventType.PRE_UPDATE, listener);
    }
}
