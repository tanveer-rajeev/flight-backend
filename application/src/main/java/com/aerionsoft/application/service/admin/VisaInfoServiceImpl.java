package com.aerionsoft.application.service.admin;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.visa.VisaInfoRequest;
import com.aerionsoft.application.dto.visa.VisaInfoResponse;
import com.aerionsoft.application.entity.visa.VisaInfo;
import com.aerionsoft.application.repository.visa.VisaInfoRepository;
import com.aerionsoft.application.util.TimestampMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VisaInfoServiceImpl implements VisaInfoService {

    @Autowired
    private VisaInfoRepository visaInfoRepository;

    @Autowired
    private TimestampMapper timestampMapper;

    @Override
    @Transactional
    public VisaInfoResponse createVisaInfo(VisaInfoRequest request) {
        VisaInfo visaInfo = new VisaInfo();
        mapRequestToEntity(request, visaInfo);
        visaInfo.setCreatedAt(UserDateTimeUtil.now());
        visaInfo.setUpdatedAt(UserDateTimeUtil.now());

        VisaInfo savedVisaInfo = visaInfoRepository.save(visaInfo);
        return mapEntityToResponse(savedVisaInfo);
    }

    @Override
    public VisaInfoResponse getVisaInfoById(Long id) {
        Optional<VisaInfo> visaInfoOpt = visaInfoRepository.findById(id);
        if (visaInfoOpt.isEmpty()) {
            throw new ResourceNotFoundException("Visa info", id);
        }
        return mapEntityToResponse(visaInfoOpt.get());
    }

    @Override
    public List<VisaInfoResponse> getAllVisaInfo() {
        List<VisaInfo> visaInfoList = visaInfoRepository.findAll();
        return visaInfoList.stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<VisaInfoResponse> getVisaInfoByCountry(String country) {
        List<VisaInfo> visaInfoList = visaInfoRepository.findByCountryIgnoreCase(country);
        return visaInfoList.stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VisaInfoResponse updateVisaInfo(Long id, VisaInfoRequest request) {
        Optional<VisaInfo> visaInfoOpt = visaInfoRepository.findById(id);
        if (visaInfoOpt.isEmpty()) {
            throw new ResourceNotFoundException("Visa info", id);
        }

        VisaInfo visaInfo = visaInfoOpt.get();
        mapRequestToEntity(request, visaInfo);
        visaInfo.setUpdatedAt(UserDateTimeUtil.now());

        VisaInfo updatedVisaInfo = visaInfoRepository.save(visaInfo);
        return mapEntityToResponse(updatedVisaInfo);
    }

    @Override
    @Transactional
    public void deleteVisaInfo(Long id) {
        Optional<VisaInfo> visaInfoOpt = visaInfoRepository.findById(id);
        if (visaInfoOpt.isEmpty()) {
            throw new ResourceNotFoundException("Visa info", id);
        }
        visaInfoRepository.delete(visaInfoOpt.get());
    }

    private void mapRequestToEntity(VisaInfoRequest request, VisaInfo visaInfo) {
        visaInfo.setFormId(request.getFormId());
        visaInfo.setCountry(request.getCountry());
        visaInfo.setVisaType(request.getVisaType());
        visaInfo.setDescription(request.getDescription());
        visaInfo.setRequiredDocuments(request.getRequiredDocuments() != null ?
            request.getRequiredDocuments().toArray(new String[0]) : null);
        visaInfo.setRules(request.getRules());
        visaInfo.setProcessingTime(request.getProcessingTime());
        visaInfo.setFeeAmount(request.getFeeAmount());
        visaInfo.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        visaInfo.setBanner(request.getBanner());
    }

    private VisaInfoResponse mapEntityToResponse(VisaInfo visaInfo) {
        VisaInfoResponse response = new VisaInfoResponse();
        response.setId(visaInfo.getId());
        response.setFormId(visaInfo.getFormId());
        response.setCountry(visaInfo.getCountry());
        response.setVisaType(visaInfo.getVisaType());
        response.setDescription(visaInfo.getDescription());
        response.setRequiredDocuments(visaInfo.getRequiredDocuments() != null ?
            List.of(visaInfo.getRequiredDocuments()) : null);
        response.setRules(visaInfo.getRules());
        response.setProcessingTime(visaInfo.getProcessingTime());
        response.setFeeAmount(visaInfo.getFeeAmount());
        response.setCurrency(visaInfo.getCurrency());
        response.setCreatedAt(timestampMapper.toRequestUserTime(visaInfo.getCreatedAt(), null));
        response.setUpdatedAt(timestampMapper.toRequestUserTime(visaInfo.getUpdatedAt(), null));
        response.setBanner(visaInfo.getBanner());
        return response;
    }
}
