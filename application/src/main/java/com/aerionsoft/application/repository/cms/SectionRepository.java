package com.aerionsoft.application.repository.cms;

import com.aerionsoft.application.entity.cms.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {
    
    @Query("SELECT s FROM Section s WHERE s.title LIKE %:keyword% OR s.description LIKE %:keyword%")
    List<Section> searchByKeyword(@Param("keyword") String keyword);
}


