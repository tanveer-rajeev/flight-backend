package com.aerionsoft.application.repository.cms;

import com.aerionsoft.application.entity.cms.Content;
import com.aerionsoft.application.enums.cms.ContentStatus;
import com.aerionsoft.application.enums.cms.ContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID> {
    Optional<Content> findBySlug(String slug);
    
    List<Content> findByType(ContentType type);
    
    List<Content> findByStatus(ContentStatus status);
    
    List<Content> findByTypeAndStatus(ContentType type, ContentStatus status);
    
    Page<Content> findByTypeAndStatus(ContentType type, ContentStatus status, Pageable pageable);
    
    @Query("SELECT c FROM Content c WHERE c.title LIKE %:keyword% OR c.description LIKE %:keyword%")
    List<Content> searchByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT c FROM Content c WHERE c.metaTitle LIKE %:keyword% OR c.metaDescription LIKE %:keyword%")
    List<Content> searchByMetaKeyword(@Param("keyword") String keyword);
    
    Boolean existsBySlug(String slug);
    
    Boolean existsBySlugAndIdNot(String slug, UUID id);
    
    Optional<Content> findByTypeAndSlug(ContentType type, String slug);
    
    Optional<Content> findByTypeAndSlugAndStatus(ContentType type, String slug, ContentStatus status);
}


