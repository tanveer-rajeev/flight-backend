package com.aerionsoft.application.service.admin;

import com.aerionsoft.application.dto.customform.CustomFormRequest;
import com.aerionsoft.application.dto.customform.CustomFormResponse;

import java.util.List;

public interface CustomFormService {
    CustomFormResponse createForm(CustomFormRequest request);
    CustomFormResponse getFormById(Long id);
    CustomFormResponse getFormBySlug(String slug);
    List<CustomFormResponse> getAllForms();
    CustomFormResponse updateForm(Long id, CustomFormRequest request);
    void deleteForm(Long id);
}
