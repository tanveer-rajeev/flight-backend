package com.aerionsoft.application.repository.common;

import com.aerionsoft.application.entity.Countries;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountriesRepository extends JpaRepository<Countries, Long> {
}
