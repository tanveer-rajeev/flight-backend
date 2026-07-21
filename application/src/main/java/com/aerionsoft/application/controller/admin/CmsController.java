package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.admin.cms.*;
import com.aerionsoft.application.enums.cms.ContentStatus;
import com.aerionsoft.application.enums.cms.ContentType;
import com.aerionsoft.application.service.admin.CmsService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
@RestController
@Validated
@RequestMapping("/api/admin/cms")
@PreAuthorize("hasRole('ADMIN')")
public class CmsController extends BaseController {

    @Autowired
    private CmsService cmsService;
    @PostMapping("/category")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-category-cms')")
    public ResponseEntity<BaseResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
            CategoryResponse category = cmsService.createCategory(request);
            return ResponseEntity.ok(BaseResponse.ok("Category created successfully", category));
    }

    @GetMapping("/category/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-category-cms')") //admin and user
    public ResponseEntity<BaseResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
            CategoryResponse category = cmsService.getCategoryById(id);
            return ResponseEntity.ok(BaseResponse.ok("Category retrieved successfully", category));
    }

    @GetMapping("/category")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-category-cms')") // admin and user
    public ResponseEntity<BaseResponse<List<CategoryResponse>>> getAllCategories() {
            List<CategoryResponse> categories = cmsService.getAllCategories();
            return ResponseEntity.ok(BaseResponse.ok("Categories retrieved successfully", categories));
    }

    @PutMapping("/category/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-category-cms')")
    public ResponseEntity<BaseResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {
            CategoryResponse category = cmsService.updateCategory(id, request);
            return ResponseEntity.ok(BaseResponse.ok("Category updated successfully", category));
    }

    @DeleteMapping("/category/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-category-cms')")
    public ResponseEntity<BaseResponse<String>> deleteCategory(@PathVariable Long id) {
            cmsService.deleteCategory(id);
            return ResponseEntity.ok(BaseResponse.ok("Category deleted successfully"));
    }


    @PostMapping("/tag")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-tag-cms')")
    public ResponseEntity<BaseResponse<TagResponse>> createTag(@Valid @RequestBody TagRequest request) {
            TagResponse tag = cmsService.createTag(request);
            return ResponseEntity.ok(BaseResponse.ok("Tag created successfully", tag));
    }


    @GetMapping("/tag/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-tag-cms')") // admin and user
    public ResponseEntity<BaseResponse<TagResponse>> getTagById(@PathVariable Long id) {
            TagResponse tag = cmsService.getTagById(id);
            return ResponseEntity.ok(BaseResponse.ok("Tag retrieved successfully", tag));
    }


    @GetMapping("/tag")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-tag-cms')") // admin and user
    public ResponseEntity<BaseResponse<List<TagResponse>>> getAllTags() {
            List<TagResponse> tags = cmsService.getAllTags();
            return ResponseEntity.ok(BaseResponse.ok("Tags retrieved successfully", tags));
    }

    @PutMapping("/tag/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-tag-cms')")
    public ResponseEntity<BaseResponse<TagResponse>> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody TagRequest request
    ) {
            TagResponse tag = cmsService.updateTag(id, request);
            return ResponseEntity.ok(BaseResponse.ok("Tag updated successfully", tag));
    }


    @DeleteMapping("/tag/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-tag-cms')")
    public ResponseEntity<BaseResponse<String>> deleteTag(@PathVariable Long id) {
            cmsService.deleteTag(id);
            return ResponseEntity.ok(BaseResponse.ok("Tag deleted successfully"));
    }


    @PostMapping("/content")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-content-cms')")
    public ResponseEntity<BaseResponse<ContentResponse>> createContent(@Valid @RequestBody ContentRequest request) {
            ContentResponse content = cmsService.createContent(request);
            return ResponseEntity.ok(BaseResponse.ok("Content created successfully", content));
    }

    @GetMapping("/content/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-content-cms')") // admin and user
    public ResponseEntity<BaseResponse<ContentResponse>> getContentById(@PathVariable UUID id) {
            ContentResponse content = cmsService.getContentById(id);
            return ResponseEntity.ok(BaseResponse.ok("Content retrieved successfully", content));
    }

    @GetMapping("/content/slug/{slug}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-content-cms')") // admin and user
    public ResponseEntity<BaseResponse<ContentResponse>> getContentBySlug(@PathVariable String slug) {
            ContentResponse content = cmsService.getContentBySlug(slug);
            return ResponseEntity.ok(BaseResponse.ok("Content retrieved successfully", content));
    }

    @GetMapping("/content")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-content-cms')") // admin and user
    public ResponseEntity<BaseResponse<List<ContentResponse>>> getAllContent() {
            List<ContentResponse> content = cmsService.getAllContent();
            return ResponseEntity.ok(BaseResponse.ok("Content retrieved successfully", content));
    }

    @GetMapping("/content/type/{type}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-content-cms')") // admin and user
    public ResponseEntity<BaseResponse<List<ContentResponse>>> getContentByType(@PathVariable ContentType type) {
            List<ContentResponse> content = cmsService.getContentByType(type);
            return ResponseEntity.ok(BaseResponse.ok("Content retrieved successfully", content));
    }

    @GetMapping("/content/status/{status}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-content-status-cms')")
    public ResponseEntity<BaseResponse<List<ContentResponse>>> getContentByStatus(@PathVariable ContentStatus status) {
            List<ContentResponse> content = cmsService.getContentByStatus(status);
            return ResponseEntity.ok(BaseResponse.ok("Content retrieved successfully", content));
    }

    @GetMapping("/content/search")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-content-cms')") // admin and user
    public ResponseEntity<BaseResponse<List<ContentResponse>>> searchContent(@RequestParam String keyword) {
            List<ContentResponse> content = cmsService.searchContentByKeyword(keyword);
            return ResponseEntity.ok(BaseResponse.ok("Content search completed", content));
    }

    @PutMapping("/content/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-content-cms')")
    public ResponseEntity<BaseResponse<ContentResponse>> updateContent(
            @PathVariable UUID id,
            @Valid @RequestBody ContentRequest request
    ) {
            ContentResponse content = cmsService.updateContent(id, request);
            return ResponseEntity.ok(BaseResponse.ok("Content updated successfully", content));
    }

    @DeleteMapping("/content/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-content-cms')")
    public ResponseEntity<BaseResponse<String>> deleteContent(@PathVariable UUID id) {
            cmsService.deleteContent(id);
            return ResponseEntity.ok(BaseResponse.ok("Content deleted successfully"));
    }


    @PostMapping("/section")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-section-cms')")
    public ResponseEntity<BaseResponse<SectionResponse>> createSection(@Valid @RequestBody SectionRequest request) {
            SectionResponse section = cmsService.createSection(request);
            return ResponseEntity.ok(BaseResponse.ok("Section created successfully", section));
    }

    @GetMapping("/section/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-section-cms')") // admin and user
    public ResponseEntity<BaseResponse<SectionResponse>> getSectionById(@PathVariable Long id) {
            SectionResponse section = cmsService.getSectionById(id);
            return ResponseEntity.ok(BaseResponse.ok("Section retrieved successfully", section));
    }

    @GetMapping("/section")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-section-cms')") // admin and user
    public ResponseEntity<BaseResponse<List<SectionResponse>>> getAllSections() {
            List<SectionResponse> sections = cmsService.getAllSections();
            return ResponseEntity.ok(BaseResponse.ok("Sections retrieved successfully", sections));
    }

    @GetMapping("/section/search")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-section-cms')") // admin and user
    public ResponseEntity<BaseResponse<List<SectionResponse>>> searchSections(@RequestParam String keyword) {
            List<SectionResponse> sections = cmsService.searchSectionsByKeyword(keyword);
            return ResponseEntity.ok(BaseResponse.ok("Section search completed", sections));
    }

    @PutMapping("/section/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-section-cms')")
    public ResponseEntity<BaseResponse<SectionResponse>> updateSection(
            @PathVariable Long id,
            @Valid @RequestBody SectionRequest request
    ) {
            SectionResponse section = cmsService.updateSection(id, request);
            return ResponseEntity.ok(BaseResponse.ok("Section updated successfully", section));
    }

    @DeleteMapping("/section/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-section-cms')")
    public ResponseEntity<BaseResponse<String>> deleteSection(@PathVariable Long id) {
            cmsService.deleteSection(id);
            return ResponseEntity.ok(BaseResponse.ok("Section deleted successfully"));
    }


    @PostMapping("/content/{contentId}/section")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'add-section-to-content-cms')")
    public ResponseEntity<BaseResponse<String>> addSectionToContent(
            @PathVariable UUID contentId,
            @Valid @RequestBody ContentSectionRequest request
    ) {
            cmsService.addSectionToContent(contentId, request.getSectionId(), request.getSortOrder());
            return ResponseEntity.ok(BaseResponse.ok("Section added to content successfully"));
    }

    @GetMapping("/content/{contentId}/sections")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-content-cms')") //admin and user
    public ResponseEntity<BaseResponse<List<SectionResponse>>> getContentSections(@PathVariable UUID contentId) {
            List<SectionResponse> sections = cmsService.getContentSections(contentId);
            return ResponseEntity.ok(BaseResponse.ok("Content sections retrieved successfully", sections));
    }

    @DeleteMapping("/content/{contentId}/section/{sectionId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-section-cms')")
    public ResponseEntity<BaseResponse<String>> removeSpecificSectionFromContent(
            @PathVariable UUID contentId,
            @PathVariable Long sectionId
    ) {
            cmsService.removeSpecificSectionFromContent(contentId, sectionId);
            return ResponseEntity.ok(BaseResponse.ok("Specific section removed from content successfully"));
    }

    // Content-Category Mapping endpoints
    @PostMapping("/content/{contentId}/category/{categoryId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'add-category-to-section-cms')")
    public ResponseEntity<BaseResponse<String>> addCategoryToContent(
            @PathVariable UUID contentId,
            @PathVariable Long categoryId
    ) {
            cmsService.addCategoryToContent(contentId, categoryId);
            return ResponseEntity.ok(BaseResponse.ok("Category added to content successfully"));
    }

    @GetMapping("/content/{contentId}/category")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-content-category-cms')")
    public ResponseEntity<BaseResponse<CategoryResponse>> getContentCategory(@PathVariable UUID contentId) {
            CategoryResponse category = cmsService.getContentCategory(contentId);
            return ResponseEntity.ok(BaseResponse.ok("Content category retrieved successfully", category));
    }

    @DeleteMapping("/content/{contentId}/category")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-content-cms')")
    public ResponseEntity<BaseResponse<String>> removeCategoryFromContent(@PathVariable UUID contentId) {
            cmsService.removeCategoryFromContent(contentId);
            return ResponseEntity.ok(BaseResponse.ok("Category removed from content successfully"));
    }

    @PutMapping("/content/{contentId}/category/{categoryId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-content-cms')")
    public ResponseEntity<BaseResponse<String>> updateContentCategory(
            @PathVariable UUID contentId,
            @PathVariable Long categoryId
    ) {
            cmsService.updateContentCategory(contentId, categoryId);
            return ResponseEntity.ok(BaseResponse.ok("Content category updated successfully"));
    }

    // Content-Tag Mapping endpoints
    @PostMapping("/content/{contentId}/tag/{tagId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-content-cms')")
    public ResponseEntity<BaseResponse<String>> addTagToContent(
            @PathVariable UUID contentId,
            @PathVariable Long tagId
    ) {
            cmsService.addTagToContent(contentId, tagId);
            return ResponseEntity.ok(BaseResponse.ok("Tag added to content successfully"));
    }

    @GetMapping("/content/{contentId}/tag")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-content-tag-cms')")
    public ResponseEntity<BaseResponse<TagResponse>> getContentTag(@PathVariable UUID contentId) {
            TagResponse tag = cmsService.getContentTag(contentId);
            return ResponseEntity.ok(BaseResponse.ok("Content tag retrieved successfully", tag));
    }

    @DeleteMapping("/content/{contentId}/tag")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-content-cms')")
    public ResponseEntity<BaseResponse<String>> removeTagFromContent(@PathVariable UUID contentId) {
            cmsService.removeTagFromContent(contentId);
            return ResponseEntity.ok(BaseResponse.ok("Tag removed from content successfully"));
    }

    @PutMapping("/content/{contentId}/tag/{tagId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-content-cms')")
    public ResponseEntity<BaseResponse<String>> updateContentTag(
            @PathVariable UUID contentId,
            @PathVariable Long tagId
    ) {
            cmsService.updateContentTag(contentId, tagId);
            return ResponseEntity.ok(BaseResponse.ok("Content tag updated successfully"));
    }
}
