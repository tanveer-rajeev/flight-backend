package com.aerionsoft.application.service.tour;

import com.aerionsoft.application.dto.tour.TourPackageTypeRequest;
import com.aerionsoft.application.dto.tour.TourPackageTypeResponse;
import com.aerionsoft.application.entity.tour.TourPackageType;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.repository.tour.TourPackageRepository;
import com.aerionsoft.application.repository.tour.TourPackageTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TourPackageTypeServiceImpl implements TourPackageTypeService {

    private final TourPackageTypeRepository tourPackageTypeRepository;
    private final TourPackageRepository tourPackageRepository;

    @Override
    @Transactional
    public TourPackageTypeResponse create(TourPackageTypeRequest request) {
        TourPackageType type = new TourPackageType();
        mapRequestToEntity(request, type);
        return mapEntityToResponse(tourPackageTypeRepository.save(type));
    }

    @Override
    @Transactional
    public TourPackageTypeResponse update(Long id, TourPackageTypeRequest request) {
        TourPackageType type = tourPackageTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour package type", id));
        mapRequestToEntity(request, type);
        return mapEntityToResponse(tourPackageTypeRepository.save(type));
    }

    @Override
    public TourPackageTypeResponse getById(Long id) {
        return tourPackageTypeRepository.findById(id)
                .map(this::mapEntityToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Tour package type", id));
    }

    @Override
    public List<TourPackageTypeResponse> getAll(Boolean activeOnly) {
        List<TourPackageType> types = Boolean.TRUE.equals(activeOnly)
                ? tourPackageTypeRepository.findByIsActiveTrueOrderByNameAsc()
                : tourPackageTypeRepository.findAll();
        return types.stream().map(this::mapEntityToResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!tourPackageTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tour package type", id);
        }
        if (tourPackageRepository.countByType_Id(id) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Cannot delete tour package type that is assigned to tour packages");
        }
        tourPackageTypeRepository.deleteById(id);
    }

    @Override
    public TourPackageType resolveActiveType(Long typeId) {
        TourPackageType type = tourPackageTypeRepository.findById(typeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour package type", typeId));
        if (!Boolean.TRUE.equals(type.getIsActive())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tour package type is inactive");
        }
        return type;
    }

    private void mapRequestToEntity(TourPackageTypeRequest request, TourPackageType entity) {
        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
    }

    private TourPackageTypeResponse mapEntityToResponse(TourPackageType entity) {
        TourPackageTypeResponse response = new TourPackageTypeResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
