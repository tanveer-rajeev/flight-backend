package com.aerionsoft.application.repository.cms;

import com.aerionsoft.application.entity.cms.ContentCategoryMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentCategoryMapRepository extends JpaRepository<ContentCategoryMap, Long> {
    Optional<ContentCategoryMap> findByContentId(UUID contentId);
    
    @Query("SELECT ccm FROM ContentCategoryMap ccm WHERE ccm.content.id = :contentId")
    Optional<ContentCategoryMap> findByContentIdWithCategory(@Param("contentId") UUID contentId);
    
    void deleteByContentId(UUID contentId);
    
    void deleteByCategoryId(Long categoryId);
}


