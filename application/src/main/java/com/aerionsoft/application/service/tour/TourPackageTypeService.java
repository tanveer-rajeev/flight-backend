package com.aerionsoft.application.service.tour;

import com.aerionsoft.application.dto.tour.TourPackageTypeRequest;
import com.aerionsoft.application.dto.tour.TourPackageTypeResponse;
import com.aerionsoft.application.entity.tour.TourPackageType;

import java.util.List;

public interface TourPackageTypeService {

    TourPackageTypeResponse create(TourPackageTypeRequest request);

    TourPackageTypeResponse update(Long id, TourPackageTypeRequest request);

    TourPackageTypeResponse getById(Long id);

    List<TourPackageTypeResponse> getAll(Boolean activeOnly);

    void delete(Long id);

    TourPackageType resolveActiveType(Long typeId);
}
