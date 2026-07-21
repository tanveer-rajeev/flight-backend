package com.aerionsoft.application.repository.cms;

import com.aerionsoft.application.entity.PackageItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageItemRepository extends JpaRepository<PackageItem, Long> {

    List<PackageItem> findByPackageIdOrderBySortOrder(Long packageId);

    List<PackageItem> findByPackageIdAndItemType(Long packageId, PackageItem.ItemType itemType);

    void deleteByPackageId(Long packageId);
}
