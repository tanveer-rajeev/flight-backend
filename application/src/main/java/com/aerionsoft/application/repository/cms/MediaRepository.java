package com.aerionsoft.application.repository.cms;

import com.aerionsoft.application.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {

    List<Media> findByPackageIdOrderBySortOrder(Long packageId);

    List<Media> findByPackageIdAndFileType(Long packageId, Media.FileType fileType);

    void deleteByPackageId(Long packageId);

    long countByPackageId(Long packageId);
}
