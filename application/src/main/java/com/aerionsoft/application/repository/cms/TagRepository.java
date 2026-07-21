package com.aerionsoft.application.repository.cms;

import com.aerionsoft.application.entity.cms.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByTitle(String title);

    @Query("SELECT t FROM Tag t WHERE t.title LIKE %:title%")
    List<Tag> findByTitleContaining(@Param("title") String title);

    Boolean existsByTitle(String title);

    Boolean existsByTitleAndIdNot(String title, Long id);
}
