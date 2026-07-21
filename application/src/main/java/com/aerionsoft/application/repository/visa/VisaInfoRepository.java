package com.aerionsoft.application.repository.visa;

import com.aerionsoft.application.entity.visa.VisaInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisaInfoRepository extends JpaRepository<VisaInfo, Long> {
    List<VisaInfo> findByCountryIgnoreCase(String country);
}
