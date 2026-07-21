package com.aerionsoft.application.repository.cms;

import com.aerionsoft.application.entity.cms.ContentTagMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentTagMapRepository extends JpaRepository<ContentTagMap, Long> {
    Optional<ContentTagMap> findByContentId(UUID contentId);
    
    @Query("SELECT ctm FROM ContentTagMap ctm WHERE ctm.content.id = :contentId")
    Optional<ContentTagMap> findByContentIdWithTag(@Param("contentId") UUID contentId);
    
    void deleteByContentId(UUID contentId);
    
    void deleteByTagId(Long tagId);
}


