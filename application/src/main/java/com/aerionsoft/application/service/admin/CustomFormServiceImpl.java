package com.aerionsoft.application.service.admin;

import com.aerionsoft.application.dto.customform.*;
import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.entity.cms.CustomForm;
import com.aerionsoft.application.entity.cms.CustomFormField;
import com.aerionsoft.application.entity.cms.CustomFormSection;
import com.aerionsoft.application.repository.cms.CustomFormRepository;
import com.aerionsoft.application.util.TimestampMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomFormServiceImpl implements CustomFormService {

    @Autowired
    private CustomFormRepository customFormRepository;

    @Autowired
    private TimestampMapper timestampMapper;

    @Override
    @Transactional
    public CustomFormResponse createForm(CustomFormRequest request) {
        CustomForm form = new CustomForm();
        form.setTitle(request.getTitle());
        form.setSlug(generateSlug(request.getTitle()));
        form.setBannerImage(request.getBannerImage());
        form.setDescription(request.getDescription());
        form.setFormStatus(0);
        form.setCreatedAt(UserDateTimeUtil.now());
        form.setUpdatedAt(UserDateTimeUtil.now());

        // Create sections
        if (request.getSections() != null) {
            for (CustomFormSectionRequest sectionRequest : request.getSections()) {
                form.getSections().add(buildSection(form, sectionRequest));
            }
        }

        CustomForm savedForm = customFormRepository.save(form);
        return mapToResponse(savedForm);
    }

    @Override
    public CustomFormResponse getFormById(Long id) {
        Optional<CustomForm> formOpt = customFormRepository.findById(id);
        if (formOpt.isEmpty()) {
            throw new ResourceNotFoundException("Form", id);
        }
        return mapToResponse(formOpt.get());
    }



    @Override
    public CustomFormResponse getFormBySlug(String slug) {
        Optional<CustomForm> formOpt = customFormRepository.findBySlug(slug);
        if (formOpt.isEmpty()) {
            throw ServiceExceptions.notFound("Form not found with slug: " + slug);
        }
        return mapToResponse(formOpt.get());
    }

    @Override
    public List<CustomFormResponse> getAllForms() {
        List<CustomForm> forms = customFormRepository.findAll();
        return forms.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomFormResponse updateForm(Long id, CustomFormRequest request) {
        CustomForm form = customFormRepository.findWithSectionsAndFieldsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Form", id));

        form.setTitle(request.getTitle());
        form.setBannerImage(request.getBannerImage());
        form.setDescription(request.getDescription());
        form.setUpdatedAt(UserDateTimeUtil.now());

        syncSections(form, request.getSections());

        CustomForm savedForm = customFormRepository.save(form);
        return mapToResponse(savedForm);
    }

    private void syncSections(CustomForm form, List<CustomFormSectionRequest> sectionRequests) {
        if (sectionRequests == null) {
            form.getSections().clear();
            return;
        }

        Map<Long, CustomFormSection> existingSections = form.getSections().stream()
                .filter(section -> section.getId() != null)
                .collect(Collectors.toMap(CustomFormSection::getId, section -> section));

        Set<Long> retainedSectionIds = new HashSet<>();
        List<CustomFormSection> newSections = new ArrayList<>();

        for (CustomFormSectionRequest sectionRequest : sectionRequests) {
            CustomFormSection section;
            if (sectionRequest.getId() != null && existingSections.containsKey(sectionRequest.getId())) {
                section = existingSections.get(sectionRequest.getId());
                retainedSectionIds.add(sectionRequest.getId());
            } else {
                section = new CustomFormSection();
                section.setForm(form);
                newSections.add(section);
            }

            applySectionRequest(section, sectionRequest);
            syncFields(section, sectionRequest.getFields());
        }

        form.getSections().removeIf(section ->
                section.getId() != null && !retainedSectionIds.contains(section.getId()));
        form.getSections().addAll(newSections);
    }

    private void syncFields(CustomFormSection section, List<CustomFormFieldRequest> fieldRequests) {
        if (fieldRequests == null) {
            section.getFields().clear();
            return;
        }

        Map<Long, CustomFormField> existingFields = section.getFields().stream()
                .filter(field -> field.getId() != null)
                .collect(Collectors.toMap(CustomFormField::getId, field -> field));

        Set<Long> retainedFieldIds = new HashSet<>();
        List<CustomFormField> newFields = new ArrayList<>();

        for (CustomFormFieldRequest fieldRequest : fieldRequests) {
            CustomFormField field;
            if (fieldRequest.getId() != null && existingFields.containsKey(fieldRequest.getId())) {
                field = existingFields.get(fieldRequest.getId());
                retainedFieldIds.add(fieldRequest.getId());
            } else {
                field = new CustomFormField();
                field.setSection(section);
                newFields.add(field);
            }

            applyFieldRequest(field, fieldRequest);
        }

        section.getFields().removeIf(field ->
                field.getId() != null && !retainedFieldIds.contains(field.getId()));
        section.getFields().addAll(newFields);
    }

    private void applySectionRequest(CustomFormSection section, CustomFormSectionRequest request) {
        section.setSectionTitle(request.getTitle());
        section.setSectionDescription(request.getDescription());
        section.setSortOrder(request.getOrderIndex() != null ? request.getOrderIndex() : 0);
    }

    private void applyFieldRequest(CustomFormField field, CustomFormFieldRequest request) {
        field.setFieldName(request.getFieldName());
        field.setFieldLabel(request.getLabel());
        field.setFieldType(request.getFieldType());
        field.setPlaceholder(request.getPlaceholder());
        field.setIsRequired(request.getIsRequired());
        field.setOptions(request.getOptions() != null ?
                request.getOptions().toArray(new String[0]) : null);
        field.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
    }

    private CustomFormSection buildSection(CustomForm form, CustomFormSectionRequest sectionRequest) {
        CustomFormSection section = new CustomFormSection();
        section.setForm(form);
        applySectionRequest(section, sectionRequest);

        if (sectionRequest.getFields() != null) {
            for (CustomFormFieldRequest fieldRequest : sectionRequest.getFields()) {
                CustomFormField field = new CustomFormField();
                field.setSection(section);
                applyFieldRequest(field, fieldRequest);
                section.getFields().add(field);
            }
        }

        return section;
    }

    @Override
    @Transactional
    public void deleteForm(Long id) {
        Optional<CustomForm> formOpt = customFormRepository.findById(id);
        if (formOpt.isEmpty()) {
            throw new ResourceNotFoundException("Form", id);
        }
        customFormRepository.delete(formOpt.get());
    }

    private CustomFormResponse mapToResponse(CustomForm form) {
        CustomFormResponse response = new CustomFormResponse();
        response.setId(form.getId());
        response.setTitle(form.getTitle());
        response.setSlug(form.getSlug());
        response.setBannerImage(form.getBannerImage());
        response.setDescription(form.getDescription());
        response.setFormStatus(form.getFormStatus());
        response.setCreatedAt(timestampMapper.toRequestUserTime(form.getCreatedAt(), null));
        response.setUpdatedAt(timestampMapper.toRequestUserTime(form.getUpdatedAt(), null));

        // Map sections (avoiding circular reference)
        if (form.getSections() != null) {
            List<CustomFormSectionResponse> sectionResponses = form.getSections().stream()
                .map(this::mapSectionToResponse)
                .collect(Collectors.toList());
            response.setSections(sectionResponses);
        }

        return response;
    }

    private CustomFormSectionResponse mapSectionToResponse(CustomFormSection section) {
        CustomFormSectionResponse response = new CustomFormSectionResponse();
        response.setId(section.getId());
        response.setSectionTitle(section.getSectionTitle());
        response.setSectionDescription(section.getSectionDescription());
        response.setSortOrder(section.getSortOrder());

        // Map fields
        if (section.getFields() != null) {
            List<CustomFormFieldResponse> fieldResponses = section.getFields().stream()
                .map(this::mapFieldToResponse)
                .collect(Collectors.toList());
            response.setFields(fieldResponses);
        }

        return response;
    }

    private CustomFormFieldResponse mapFieldToResponse(CustomFormField field) {
        CustomFormFieldResponse response = new CustomFormFieldResponse();
        response.setId(field.getId());
        response.setFieldName(field.getFieldName());
        response.setLabel(field.getFieldLabel());
        response.setFieldType(field.getFieldType());
        response.setIsRequired(field.getIsRequired());
        response.setPlaceholder(field.getPlaceholder());
        response.setSortOrder(field.getSortOrder());

        // Convert array to list
        if (field.getOptions() != null) {
            response.setOptions(List.of(field.getOptions()));
        }

        return response;
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }
}
