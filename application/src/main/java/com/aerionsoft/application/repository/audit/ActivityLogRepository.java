package com.aerionsoft.application.repository.audit;



import com.aerionsoft.application.entity.audit.ActivityLog;

import com.aerionsoft.application.enums.audit.ActorType;

import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.stereotype.Repository;

import com.aerionsoft.application.enums.audit.ActivityEventCategory;

import java.util.Collection;

import java.util.List;



@Repository

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long>, JpaSpecificationExecutor<ActivityLog> {



    Page<ActivityLog> findByActorTypeAndActorIdOrderByCreatedAtDesc(

            ActorType actorType, Long actorId, Pageable pageable);

    List<ActivityLog> findByIdGreaterThanAndEventCategoryInOrderByIdAsc(

            Long id, Collection<ActivityEventCategory> eventCategories, Pageable pageable);

    List<ActivityLog> findByEventCategoryInOrderByIdDesc(

            Collection<ActivityEventCategory> eventCategories, Pageable pageable);

}

