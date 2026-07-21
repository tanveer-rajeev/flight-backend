package com.aerionsoft.application.service.admin;

import com.aerionsoft.application.dto.admin.cms.*;
import com.aerionsoft.application.entity.cms.*;
import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.enums.cms.ContentStatus;
import com.aerionsoft.application.enums.cms.ContentType;
import com.aerionsoft.application.repository.cms.CategoryRepository;
import com.aerionsoft.application.repository.cms.ContentCategoryMapRepository;
import com.aerionsoft.application.repository.cms.ContentRepository;
import com.aerionsoft.application.repository.cms.ContentSectionMapRepository;
import com.aerionsoft.application.repository.cms.ContentTagMapRepository;
import com.aerionsoft.application.repository.cms.SectionRepository;
import com.aerionsoft.application.repository.cms.TagRepository;
import com.aerionsoft.application.util.TimestampMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CmsService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private ContentSectionMapRepository contentSectionMapRepository;

    @Autowired
    private ContentCategoryMapRepository contentCategoryMapRepository;

    @Autowired
    private ContentTagMapRepository contentTagMapRepository;

    @Autowired
    private TimestampMapper timestampMapper;

    // Category CRUD operations
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByTitle(request.getTitle())) {
            throw ServiceExceptions.notFound("Category with title '" + request.getTitle() + "' already exists");
        }

        Category category = Category.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        category = categoryRepository.save(category);
        return mapToCategoryResponse(category);
    }

    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return mapToCategoryResponse(category);
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        if (!category.getTitle().equals(request.getTitle()) &&
                categoryRepository.existsByTitleAndIdNot(request.getTitle(), id)) {
            throw ServiceExceptions.notFound("Category with title '" + request.getTitle() + "' already exists");
        }

        category.setTitle(request.getTitle());
        category.setDescription(request.getDescription());

        category = categoryRepository.save(category);
        return mapToCategoryResponse(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        // Delete category
        categoryRepository.delete(category);
    }

    // Tag CRUD operations
    @Transactional
    public TagResponse createTag(TagRequest request) {
        if (tagRepository.existsByTitle(request.getTitle())) {
            throw ServiceExceptions.notFound("Tag with title '" + request.getTitle() + "' already exists");
        }

        Tag tag = Tag.builder()
                .title(request.getTitle())
                .build();

        tag = tagRepository.save(tag);
        return mapToTagResponse(tag);
    }

    public TagResponse getTagById(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", id));
        return mapToTagResponse(tag);
    }

    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream()
                .map(this::mapToTagResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TagResponse updateTag(Long id, TagRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", id));

        if (!tag.getTitle().equals(request.getTitle()) &&
                tagRepository.existsByTitleAndIdNot(request.getTitle(), id)) {
            throw ServiceExceptions.notFound("Tag with title '" + request.getTitle() + "' already exists");
        }

        tag.setTitle(request.getTitle());

        tag = tagRepository.save(tag);
        return mapToTagResponse(tag);
    }

    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", id));

        // Delete tag
        tagRepository.delete(tag);
    }

    // Content CRUD operations
    @Transactional
    public ContentResponse createContent(ContentRequest request) {
        if (contentRepository.existsBySlug(request.getSlug())) {
            throw ServiceExceptions.notFound("Content with slug '" + request.getSlug() + "' already exists");
        }

        // Validate BLOG content requirements
        if (request.getType() == ContentType.BLOG) {
            if (request.getCategoryId() == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Category is required for BLOG content type");
            }
            if (request.getTagId() == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Tag is required for BLOG content type");
            }
        }

        Content content = Content.builder()
                .type(request.getType())
                .title(request.getTitle())
                .slug(request.getSlug())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .metaTitle(request.getMetaTitle())
                .metaDescription(request.getMetaDescription())
                .status(request.getStatus())
                .build();

        Content savedContent = contentRepository.save(content);

        // Handle sections if provided - create sections internally
        if (request.getSections() != null && !request.getSections().isEmpty()) {
            int sortOrder = 1;
            for (SectionRequest sectionRequest : request.getSections()) {
                // Create the section first
                Section section = Section.builder()
                        .title(sectionRequest.getTitle())
                        .description(sectionRequest.getDescription())
                        .imageUrl(sectionRequest.getImageUrl())
                        .build();
                
                Section savedSection = sectionRepository.save(section);
                
                // Add section to content with sort order
                addSectionToContent(savedContent.getId(), savedSection.getId(), sortOrder++);
            }
        }

        // Handle category if provided
        if (request.getCategoryId() != null) {
            addCategoryToContent(savedContent.getId(), request.getCategoryId());
        }

        // Handle tag if provided
        if (request.getTagId() != null) {
            addTagToContent(savedContent.getId(), request.getTagId());
        }

        return convertToContentResponse(savedContent);
    }

    public ContentResponse getContentById(UUID id) {
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content", id));
        return convertToContentResponse(content);
    }

    public ContentResponse getContentBySlug(String slug) {
        Content content = contentRepository.findBySlug(slug)
                .orElseThrow(() -> ServiceExceptions.notFound("Content not found with slug: " + slug));
        return convertToContentResponse(content);
    }

    public ContentResponse getContentByTypeAndSlug(ContentType type, String slug) {
        Content content = contentRepository.findByTypeAndSlugAndStatus(type, slug, ContentStatus.ACTIVE)
                .orElseThrow(() -> ServiceExceptions.notFound("Active content not found with type '" + type + "' and slug: " + slug));
        return convertToContentResponse(content);
    }

    public List<ContentResponse> getAllContent() {
        List<Content> contents = contentRepository.findAll();
        return contents.stream()
                .map(this::convertToContentResponse)
                .collect(Collectors.toList());
    }

    public List<PageListItemResponse> getAllPagesList() {
        List<Content> contents = contentRepository.findAll();
        return contents.stream()
                .map(content -> PageListItemResponse.builder()
                        .title(content.getTitle())
                        .slug(content.getSlug())
                        .type(content.getType())
                        .build())
                .collect(Collectors.toList());
    }

    public List<ContentResponse> getContentByType(ContentType type) {
        List<Content> contents = contentRepository.findByType(type);
        return contents.stream()
                .map(this::convertToContentResponse)
                .collect(Collectors.toList());
    }

    public List<ContentResponse> getContentByStatus(ContentStatus status) {
        List<Content> contents = contentRepository.findByStatus(status);
        return contents.stream()
                .map(this::convertToContentResponse)
                .collect(Collectors.toList());
    }

    public List<ContentResponse> searchContentByKeyword(String keyword) {
        List<Content> contents = contentRepository.searchByKeyword(keyword);
        return contents.stream()
                .map(this::convertToContentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContentResponse updateContent(UUID id, ContentRequest request) {
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content", id));

        if (!content.getSlug().equals(request.getSlug()) &&
                contentRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
            throw ServiceExceptions.notFound("Content with slug '" + request.getSlug() + "' already exists");
        }

        // Validate BLOG content requirements
        if (request.getType() == ContentType.BLOG) {
            if (request.getCategoryId() == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Category is required for BLOG content type");
            }
            if (request.getTagId() == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Tag is required for BLOG content type");
            }
        }

        content.setType(request.getType());
        content.setTitle(request.getTitle());
        content.setSlug(request.getSlug());
        content.setDescription(request.getDescription());
        content.setImageUrl(request.getImageUrl());
        content.setMetaTitle(request.getMetaTitle());
        content.setMetaDescription(request.getMetaDescription());
        content.setStatus(request.getStatus());

        Content updatedContent = contentRepository.save(content);

        // Handle sections in a separate transaction to avoid conflicts
        if (request.getSections() != null && !request.getSections().isEmpty()) {
            updateContentSections(id, request.getSections());
        }

        // Handle category if provided
        if (request.getCategoryId() != null) {
            updateContentCategory(id, request.getCategoryId());
        } else {
            // Remove category if not provided
            contentCategoryMapRepository.deleteByContentId(id);
        }

        // Handle tag if provided
        if (request.getTagId() != null) {
            updateContentTag(id, request.getTagId());
        } else {
            // Remove tag if not provided
            contentTagMapRepository.deleteByContentId(id);
        }

        return convertToContentResponse(updatedContent);
    }

    @Transactional
    public void deleteContent(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));

        // Get all related entities before deleting mappings
        List<ContentSectionMap> sectionMappings = contentSectionMapRepository.findByContentIdOrderBySortOrder(contentId);
        Optional<ContentCategoryMap> categoryMapping = contentCategoryMapRepository.findByContentId(contentId);
        Optional<ContentTagMap> tagMapping = contentTagMapRepository.findByContentId(contentId);

        // Delete all related mappings first
        contentSectionMapRepository.deleteByContentId(contentId);
        contentCategoryMapRepository.deleteByContentId(contentId);
        contentTagMapRepository.deleteByContentId(contentId);

        // Delete the actual sections, category, and tag entities
        for (ContentSectionMap mapping : sectionMappings) {
            sectionRepository.delete(mapping.getSection());
        }

        if (categoryMapping.isPresent()) {
            categoryRepository.delete(categoryMapping.get().getCategory());
        }

        if (tagMapping.isPresent()) {
            tagRepository.delete(tagMapping.get().getTag());
        }

        // Finally delete the content itself
        contentRepository.delete(content);
    }

    // Section CRUD operations
    @Transactional
    public SectionResponse createSection(SectionRequest request) {
        Section section = Section.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .build();

        Section savedSection = sectionRepository.save(section);
        return convertToSectionResponse(savedSection);
    }

    public SectionResponse getSectionById(Long id) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section", id));
        return convertToSectionResponse(section);
    }

    public List<SectionResponse> getAllSections() {
        List<Section> sections = sectionRepository.findAll();
        return sections.stream()
                .map(this::convertToSectionResponse)
                .collect(Collectors.toList());
    }

    public List<SectionResponse> searchSectionsByKeyword(String keyword) {
        List<Section> sections = sectionRepository.searchByKeyword(keyword);
        return sections.stream()
                .map(this::convertToSectionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SectionResponse updateSection(Long id, SectionRequest request) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section", id));

        section.setTitle(request.getTitle());
        section.setDescription(request.getDescription());
        section.setImageUrl(request.getImageUrl());

        Section updatedSection = sectionRepository.save(section);
        return convertToSectionResponse(updatedSection);
    }

    @Transactional
    public void deleteSection(Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section", sectionId));

        // Delete all content-section mappings first
        contentSectionMapRepository.deleteBySectionId(sectionId);

        // Then delete the section itself
        sectionRepository.delete(section);
    }

    // Helper method to update content sections in a separate transaction
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateContentSections(UUID contentId, List<SectionRequest> sections) {
        try {
            // Clear existing sections first
            clearContentSections(contentId);
            
            // Create new sections
            int sortOrder = 1;
            for (SectionRequest sectionRequest : sections) {
                // Create the section first
                Section section = Section.builder()
                        .title(sectionRequest.getTitle())
                        .description(sectionRequest.getDescription())
                        .imageUrl(sectionRequest.getImageUrl())
                        .build();
                
                Section savedSection = sectionRepository.save(section);
                
                // Validate that the section was saved with an ID
                if (savedSection.getId() == null) {
                    throw ServiceExceptions.notFound("Failed to create section: ID is null");
                }
                
                // Add section to content with sort order
                addSectionToContent(contentId, savedSection.getId(), sortOrder++);
            }
        } catch (Exception e) {
            System.err.println("Error updating content sections: " + e.getMessage());
            throw ServiceExceptions.internal("Failed to update content sections", e);
        }
    }

    // Helper method to clear all sections for a content
    @Transactional
    public void clearContentSections(UUID contentId) {
        try {
            // Get all existing mappings to identify sections to delete
            List<ContentSectionMap> existingMappings = contentSectionMapRepository.findByContentIdOrderBySortOrder(contentId);
            
            if (!existingMappings.isEmpty()) {
                // First, delete all mappings to remove foreign key constraints
                contentSectionMapRepository.deleteByContentId(contentId);
                
                // Then delete all section entities (now safe to delete)
                for (ContentSectionMap mapping : existingMappings) {
                    if (mapping.getSection() != null) {
                        sectionRepository.delete(mapping.getSection());
                    }
                }
                
                // Flush to ensure deletions are committed
                contentSectionMapRepository.flush();
                sectionRepository.flush();
            }
        } catch (Exception e) {
            // Log the error but don't fail the entire operation
            System.err.println("Error clearing content sections: " + e.getMessage());
            // Try to delete mappings directly as fallback
            try {
                contentSectionMapRepository.deleteByContentId(contentId);
            } catch (Exception ex) {
                System.err.println("Fallback deletion also failed: " + ex.getMessage());
            }
        }
    }

    // Content-Section Mapping operations
    @Transactional
    public void addSectionToContent(UUID contentId, Long sectionId, Integer sortOrder) {
        // Validate inputs
        if (contentId == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Content ID cannot be null");
        }
        if (sectionId == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Section ID cannot be null");
        }
        if (sortOrder == null || sortOrder < 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Sort order must be a positive integer");
        }
        
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section", sectionId));

        ContentSectionMap mapping = ContentSectionMap.builder()
                .content(content)
                .section(section)
                .sortOrder(sortOrder)
                .build();

        contentSectionMapRepository.save(mapping);
    }

    public List<SectionResponse> getContentSections(UUID contentId) {
        List<ContentSectionMap> mappings = contentSectionMapRepository.findByContentIdOrderBySortOrder(contentId);
        return mappings.stream()
                .map(mapping -> convertToSectionResponse(mapping.getSection()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeSectionFromContent(UUID contentId) {
        contentSectionMapRepository.deleteByContentId(contentId);
    }

    @Transactional
    public void removeSpecificSectionFromContent(UUID contentId, Long sectionId) {
        contentSectionMapRepository.deleteByContentIdAndSectionId(contentId, sectionId);
    }

    @Transactional
    public void updateContentCategory(UUID contentId, Long newCategoryId) {
        // Remove existing category mapping
        contentCategoryMapRepository.deleteByContentId(contentId);
        
        // Add new category mapping
        addCategoryToContent(contentId, newCategoryId);
    }

    @Transactional
    public void updateContentTag(UUID contentId, Long newTagId) {
        // Remove existing tag mapping
        contentTagMapRepository.deleteByContentId(contentId);
        
        // Add new tag mapping
        addTagToContent(contentId, newTagId);
    }

    // Content-Category Mapping operations
    @Transactional
    public void addCategoryToContent(UUID contentId, Long categoryId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));

        ContentCategoryMap mapping = ContentCategoryMap.builder()
                .content(content)
                .category(category)
                .build();

        contentCategoryMapRepository.save(mapping);
    }

    public CategoryResponse getContentCategory(UUID contentId) {
        ContentCategoryMap mapping = contentCategoryMapRepository.findByContentId(contentId)
                .orElseThrow(() -> ServiceExceptions.notFound("Content category for content id: " + contentId));
        return convertToCategoryResponse(mapping.getCategory());
    }

    @Transactional
    public void removeCategoryFromContent(UUID contentId) {
        contentCategoryMapRepository.deleteByContentId(contentId);
    }

    // Content-Tag Mapping operations
    @Transactional
    public void addTagToContent(UUID contentId, Long tagId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", tagId));

        ContentTagMap mapping = ContentTagMap.builder()
                .content(content)
                .tag(tag)
                .build();

        contentTagMapRepository.save(mapping);
    }

    public TagResponse getContentTag(UUID contentId) {
        ContentTagMap mapping = contentTagMapRepository.findByContentId(contentId)
                .orElseThrow(() -> ServiceExceptions.notFound("Content tag for content id: " + contentId));
        return convertToTagResponse(mapping.getTag());
    }

    @Transactional
    public void removeTagFromContent(UUID contentId) {
        contentTagMapRepository.deleteByContentId(contentId);
    }

    // Mapping methods
    private CategoryResponse mapToCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .title(category.getTitle())
                .description(category.getDescription())
                .build();
    }

    private TagResponse mapToTagResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .title(tag.getTitle())
                .build();
    }

    // Content conversion methods
    private ContentResponse convertToContentResponse(Content content) {
        ContentResponse.ContentResponseBuilder builder = ContentResponse.builder()
                .id(content.getId())
                .type(content.getType())
                .title(content.getTitle())
                .slug(content.getSlug())
                .description(content.getDescription())
                .imageUrl(content.getImageUrl())
                .metaTitle(content.getMetaTitle())
                .metaDescription(content.getMetaDescription())
                .status(content.getStatus())
                .createdAt(timestampMapper.toRequestUserTime(content.getCreatedAt(), content.getCreatedTimeOffset()))
                .updatedAt(timestampMapper.toRequestUserTime(content.getUpdatedAt(), content.getUpdatedTimeOffset() != null ? content.getUpdatedTimeOffset() : content.getCreatedTimeOffset()));

        // Add sections
        List<ContentSectionMap> sectionMappings = contentSectionMapRepository.findByContentIdOrderBySortOrder(content.getId());
        List<SectionResponse> sections = sectionMappings.stream()
                .map(mapping -> convertToSectionResponse(mapping.getSection()))
                .collect(Collectors.toList());
        builder.sections(sections);

        // Add category
        Optional<ContentCategoryMap> categoryMapping = contentCategoryMapRepository.findByContentId(content.getId());
        if (categoryMapping.isPresent()) {
            builder.category(convertToCategoryResponse(categoryMapping.get().getCategory()));
        }

        // Add tag
        Optional<ContentTagMap> tagMapping = contentTagMapRepository.findByContentId(content.getId());
        if (tagMapping.isPresent()) {
            builder.tag(convertToTagResponse(tagMapping.get().getTag()));
        }

        return builder.build();
    }

    private SectionResponse convertToSectionResponse(Section section) {
        return SectionResponse.builder()
                .id(section.getId())
                .title(section.getTitle())
                .description(section.getDescription())
                .imageUrl(section.getImageUrl())
                .build();
    }

    private CategoryResponse convertToCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .title(category.getTitle())
                .description(category.getDescription())
                .build();
    }

    private TagResponse convertToTagResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .title(tag.getTitle())
                .build();
    }
}
