package com.aerionsoft.application.repository.cms;

import com.aerionsoft.application.entity.cms.ContentSectionMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentSectionMapRepository extends JpaRepository<ContentSectionMap, Long> {
    List<ContentSectionMap> findByContentIdOrderBySortOrder(UUID contentId);
    
    @Query("SELECT csm FROM ContentSectionMap csm WHERE csm.content.id = :contentId ORDER BY csm.sortOrder")
    List<ContentSectionMap> findByContentIdWithOrder(@Param("contentId") UUID contentId);
    
    void deleteByContentId(UUID contentId);
    
    void deleteBySectionId(Long sectionId);
    
    void deleteByContentIdAndSectionId(UUID contentId, Long sectionId);
}
