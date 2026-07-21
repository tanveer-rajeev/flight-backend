package com.aerionsoft.application.repository.cms;

import com.aerionsoft.application.entity.cms.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByTitle(String title);

    @Query("SELECT c FROM Category c WHERE c.title LIKE %:title%")
    List<Category> findByTitleContaining(@Param("title") String title);

    Boolean existsByTitle(String title);

    Boolean existsByTitleAndIdNot(String title, Long id);
}
