package com.aerionsoft.application.service.common;

import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.CountriesDto;
import com.aerionsoft.application.entity.Countries;
import com.aerionsoft.application.repository.common.CountriesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CountriesService {

    @Autowired
    private CountriesRepository countriesRepository;

    public List<Countries> getAll() {
        return countriesRepository.findAll();
    }

    public Optional<Countries> getById(Long id) {
        return countriesRepository.findById(id);
    }

    public Countries create(CountriesDto dto) {
        Countries country = new Countries();
        country.setCountryCode(dto.getCountryCode());
        country.setCountryName(dto.getCountryName());
        return countriesRepository.save(country);
    }

    public Countries update(Long id, CountriesDto dto) {
        Optional<Countries> existing = countriesRepository.findById(id);
        if (existing.isPresent()) {
            Countries country = existing.get();
            country.setCountryCode(dto.getCountryCode());
            country.setCountryName(dto.getCountryName());
            return countriesRepository.save(country);
        }
        throw new ResourceNotFoundException("Country");
    }

    public void delete(Long id) {
        countriesRepository.deleteById(id);
    }
}
