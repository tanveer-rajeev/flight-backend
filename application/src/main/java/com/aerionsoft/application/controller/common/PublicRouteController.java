package com.aerionsoft.application.controller.common;

import com.aerionsoft.application.service.admin.CmsService;
import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.FileUploadResponse;
import com.aerionsoft.application.dto.admin.cms.ContentResponse;
import com.aerionsoft.application.dto.admin.cms.PageListItemResponse;
import com.aerionsoft.application.dto.business.BusinessDto;
import com.aerionsoft.application.dto.business.PublicAgencyRequest;
import com.aerionsoft.application.dto.customform.CustomFormResponse;
import com.aerionsoft.application.dto.tour.TourApplicationResponse;
import com.aerionsoft.application.dto.tour.TourCategoryResponse;
import com.aerionsoft.application.dto.tour.TourPackageListItemResponse;
import com.aerionsoft.application.dto.tour.TourPackageResponse;
import com.aerionsoft.application.dto.visa.VisaApplicationResponse;
import com.aerionsoft.application.dto.visa.VisaInfoResponse;
import com.aerionsoft.application.entity.Countries;
import com.aerionsoft.application.enums.tour.ApplicationStatus;
import com.aerionsoft.application.enums.cms.ContentType;
import com.aerionsoft.application.service.business.BusinessService;
import com.aerionsoft.application.dto.admin.AirlineDto;
import com.aerionsoft.application.dto.admin.AirportDto;
import com.aerionsoft.application.dto.platform.PlatformInfoData;
import com.aerionsoft.application.service.admin.AirportAirLineService;
import com.aerionsoft.application.service.common.CountriesService;
import com.aerionsoft.application.service.common.IpRateLimiterService;
import com.aerionsoft.application.service.common.PlatformAirlineService;
import com.aerionsoft.application.service.common.PlatformInfoService;
import com.aerionsoft.application.service.common.R2FileService;
import com.aerionsoft.application.service.tour.TourApplicationService;
import com.aerionsoft.application.service.admin.AdminUserService;
import com.aerionsoft.application.service.admin.CustomFormService;
import com.aerionsoft.application.service.tour.TourCategoryService;
import com.aerionsoft.application.service.tour.TourPackageService;
import com.aerionsoft.application.service.admin.VisaInfoService;
import com.aerionsoft.application.service.visa.VisaApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@Validated
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicRouteController {

    private static final String PUBLIC_IMAGE_UPLOAD_RATE_KEY = "public-image-upload";
    private static final int PUBLIC_IMAGE_UPLOAD_MAX = 3;
    private static final Duration PUBLIC_IMAGE_UPLOAD_WINDOW = Duration.ofMinutes(5);

    @Autowired
    private VisaApplicationService visaApplicationService;

    @Autowired
    private final TourPackageService tourPackageService;

    @Autowired
    private final AdminUserService adminUserService;

    @Autowired
    private VisaInfoService visaInfoService;

    @Autowired
    private TourApplicationService tourApplicationService;
    @Autowired
    private CustomFormService customFormService;
    @Autowired
    private CmsService cmsService;

    @Autowired
    private TourCategoryService tourCategoryService;

    @Autowired
    private CountriesService countriesService;

    @Autowired
    private BusinessService businessService;

    @Autowired
    private R2FileService fileService;

    @Autowired
    private IpRateLimiterService ipRateLimiterService;

    @Autowired
    private PlatformInfoService platformInfoService;

    @Autowired
    private PlatformAirlineService platformAirlineService;

    @Autowired
    private AirportAirLineService airportAirLineService;

    @GetMapping("/platform-info")
    public ResponseEntity<BaseResponse<PlatformInfoData>> getPlatformInfo() {
        return ResponseEntity.ok(BaseResponse.ok(
                platformInfoService.getPlatformInfo(),
                "Platform information retrieved successfully"));
    }

    @GetMapping("/airports")
    public ResponseEntity<BaseResponse<List<AirportDto>>> getAirports() {
        return ResponseEntity.ok(BaseResponse.ok(airportAirLineService.getAllAirports()));
    }

    @GetMapping("/airlines")
    public ResponseEntity<BaseResponse<List<AirlineDto>>> getAirlines() {
        return ResponseEntity.ok(BaseResponse.ok(platformAirlineService.getAirlines()));
    }

    /**
     * Public agency signup. Creates a mother user from representative details
     * and a business with PENDING status for admin approval.
     */
    @PostMapping("/businesses")
    public ResponseEntity<BaseResponse<BusinessDto>> createAgency(@Valid @RequestBody PublicAgencyRequest request) {
        return ResponseEntity.ok(BaseResponse.ok("Agency request submitted successfully",
                businessService.createPublicAgency(request)));
    }

    /**
     * Public image upload. Limited to 3 uploads per IP every 5 minutes.
     */
    @PostMapping("/files/upload/image")
    public ResponseEntity<BaseResponse<FileUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "public") String folder,
            HttpServletRequest request) throws IOException {

        String ip = BaseController.getClientIp(request);
        ipRateLimiterService.checkOrThrow(
                PUBLIC_IMAGE_UPLOAD_RATE_KEY, ip, PUBLIC_IMAGE_UPLOAD_MAX, PUBLIC_IMAGE_UPLOAD_WINDOW);

        String fileUrl = fileService.uploadImage(file, folder);
        FileUploadResponse data = FileUploadResponse.builder()
                .fileUrl(fileUrl)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .uploadedBy("public:" + ip)
                .type("image")
                .build();

        return ResponseEntity.ok(BaseResponse.ok("Image uploaded successfully", data));
    }

    @GetMapping("country/list")
    public ResponseEntity<BaseResponse<List<Countries>>> list() {
        return ResponseEntity.ok(BaseResponse.ok(countriesService.getAll()));
    }

    @GetMapping("country/{id}")
    public ResponseEntity<BaseResponse<Countries>> getById(@PathVariable Long id) {
        Optional<Countries> country = countriesService.getById(id);
        return country.map(countries -> ResponseEntity.ok(BaseResponse.ok(countries))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/visa-info/{id}")
    public ResponseEntity<BaseResponse<VisaInfoResponse>> getVisaInfo(@PathVariable Long id) {
        VisaInfoResponse visaInfo = visaInfoService.getVisaInfoById(id);
        return ResponseEntity.ok(BaseResponse.ok(visaInfo, "Visa info retrieved successfully"));
    }

    @GetMapping("/visa-info")
    public ResponseEntity<BaseResponse<List<VisaInfoResponse>>> getAllVisaInfo() {
        List<VisaInfoResponse> visaInfoList = visaInfoService.getAllVisaInfo();
        return ResponseEntity.ok(BaseResponse.ok(visaInfoList, "Visa info list retrieved successfully"));
    }

    @GetMapping("/visa-info/country/{country}")
    public ResponseEntity<BaseResponse<List<VisaInfoResponse>>> getVisaInfoByCountry(@PathVariable String country) {
        List<VisaInfoResponse> visaInfoList = visaInfoService.getVisaInfoByCountry(country);
        return ResponseEntity.ok(BaseResponse.ok(visaInfoList, "Visa info for country retrieved successfully"));
    }



    @GetMapping("/visa-application/{id}")
    public ResponseEntity<BaseResponse<VisaApplicationResponse>> getApplication(@PathVariable Long id) {
        VisaApplicationResponse application = visaApplicationService.getApplicationById(id);
        return ResponseEntity.ok(BaseResponse.ok(application, "Visa application retrieved successfully"));
    }

    @GetMapping("/visa-applications")
    public ResponseEntity<BaseResponse<List<VisaApplicationResponse>>> getVisaAllApplications(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String visaType,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime submittedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime submittedTo,
            @RequestParam(required = false) String processedBy,
            @RequestParam(required = false) Long visaId) {

        // If any filter parameters are provided, use filtered search
        if (status != null || visaType != null || country != null ||
                submittedFrom != null || submittedTo != null || processedBy != null || visaId != null) {
            List<VisaApplicationResponse> applications = visaApplicationService.getAllApplicationsWithFilters(
                    status, visaType, country, submittedFrom, submittedTo, processedBy, visaId);
            return ResponseEntity.ok(BaseResponse.ok(applications, "Filtered visa applications retrieved successfully"));
        }

        List<VisaApplicationResponse> applications = visaApplicationService.getAllApplications();
        return ResponseEntity.ok(BaseResponse.ok(applications, "Visa applications retrieved successfully"));
    }

    @GetMapping("/tour-package/{id}")
    public ResponseEntity<BaseResponse<TourPackageResponse>> getTourPackageById(@PathVariable Long id) {
        tourPackageService.recordTourView(id);
        TourPackageResponse response = tourPackageService.getTourPackageById(id);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour package retrieved successfully"));
    }

    @GetMapping("/tour-packages")
    public ResponseEntity<BaseResponse<Page<TourPackageResponse>>> getAllTourPackages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TourPackageResponse> response = tourPackageService.getAllTourPackages(pageable);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour packages retrieved successfully"));
    }

    @GetMapping("/tour-packages/list")
    public ResponseEntity<BaseResponse<Page<TourPackageListItemResponse>>> getAllTourPackagesList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TourPackageListItemResponse> response = tourPackageService.getAllTourPackagesList(pageable);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour packages list retrieved successfully"));
    }

    @GetMapping("/tour-package/destination")
    public ResponseEntity<BaseResponse<List<TourPackageResponse>>> getTourPackagesByDestination(
            @RequestParam String country,
            @RequestParam(required = false) String city) {
        List<TourPackageResponse> response = tourPackageService.getTourPackagesByDestination(country, city);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour packages by destination retrieved successfully"));
    }

    @GetMapping("/tour-package/date-range")
    public ResponseEntity<BaseResponse<List<TourPackageResponse>>> getTourPackagesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TourPackageResponse> response = tourPackageService.getTourPackagesByDateRange(startDate, endDate);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour packages by date range retrieved successfully"));
    }

    @GetMapping("/tour-package/search")
    public ResponseEntity<BaseResponse<Page<TourPackageResponse>>> searchTourPackages(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<TourPackageResponse> response = tourPackageService.searchPublishedTourPackages(keyword, pageable);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour packages search results retrieved successfully"));
    }

    @GetMapping("/tour-package/popular")
    public ResponseEntity<BaseResponse<List<TourPackageResponse>>> getPopularTourPackages() {
        List<TourPackageResponse> response = tourPackageService.getPopularPublishedTourPackages();
        return ResponseEntity.ok(BaseResponse.ok(response, "Popular tour packages retrieved successfully"));
    }



    @GetMapping("/tour-application/{id}")
    public ResponseEntity<BaseResponse<TourApplicationResponse>> getApplicationTourApplication(@PathVariable Long id) {
            TourApplicationResponse application = tourApplicationService.getApplicationById(id);
            return ResponseEntity.ok(BaseResponse.ok(application, "Tour application retrieved successfully"));
    }

    @GetMapping("/tour-applications")
    public ResponseEntity<BaseResponse<List<TourApplicationResponse>>> getAllApplicationsTourApplication(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) Long tourId,
            @RequestParam(required = false) Long formId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime submittedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime submittedTo,
            @RequestParam(required = false) String processedBy) {

        // If any filter parameters are provided, use Specification-based filtered search
        if (status != null || tourId != null || formId != null ||
                submittedFrom != null || submittedTo != null || processedBy != null) {
            List<TourApplicationResponse> applications = tourApplicationService.getAllApplicationsWithAdvancedFilters(
                    status, tourId, formId, submittedFrom, submittedTo, processedBy);
            return ResponseEntity.ok(BaseResponse.ok(applications, "Filtered tour applications retrieved successfully"));
        }

        // Otherwise, use the existing method for backward compatibility
        List<TourApplicationResponse> applications = tourApplicationService.getAllTourApplications();
        return ResponseEntity.ok(BaseResponse.ok(applications, "Tour applications retrieved successfully"));
    }

    @GetMapping("forms/{id}")
    public ResponseEntity<BaseResponse<CustomFormResponse>> getForm(@PathVariable Long id) {
        CustomFormResponse form = customFormService.getFormById(id);
        return ResponseEntity.ok(BaseResponse.ok(form, "Form retrieved successfully"));
    }

    @GetMapping("forms/slug/{slug}")
    public ResponseEntity<BaseResponse<CustomFormResponse>> getFormBySlug(@PathVariable String slug) {
        CustomFormResponse form = customFormService.getFormBySlug(slug);
        return ResponseEntity.ok(BaseResponse.ok(form, "Form retrieved successfully"));
    }

    @GetMapping("/cms/content/by-type-and-slug")
    public ResponseEntity<BaseResponse<ContentResponse>> getContentByTypeAndSlug(
            @RequestParam ContentType type,
            @RequestParam String slug) {
            ContentResponse content = cmsService.getContentByTypeAndSlug(type, slug);
            return ResponseEntity.ok(BaseResponse.ok(content, "Content retrieved successfully"));
    }

    @GetMapping("/cms/pages/list")
    public ResponseEntity<BaseResponse<List<PageListItemResponse>>> getAllPagesList() {
        List<PageListItemResponse> pages = cmsService.getAllPagesList();
        return ResponseEntity.ok(BaseResponse.ok(pages, "Pages list retrieved successfully"));
    }

    // ── Tour flags ─────────────────────────────────────────────────────────────

    @GetMapping("/tour-packages/by-flag")
    public ResponseEntity<BaseResponse<List<TourPackageResponse>>> getTourPackagesByFlag(
            @RequestParam String flag) {
        List<TourPackageResponse> response = tourPackageService.getPublishedTourPackagesByFlag(flag);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour packages by flag retrieved successfully"));
    }

    // ── Tour view count ────────────────────────────────────────────────────────

    @GetMapping("/tour-package/{id}/views")
    public ResponseEntity<BaseResponse<Long>> getTourViewCount(@PathVariable Long id) {
        Long count = tourPackageService.getTourViewCount(id);
        return ResponseEntity.ok(BaseResponse.ok(count, "Tour view count retrieved successfully"));
    }

    // ── Tour categories (public read) ──────────────────────────────────────────

    @GetMapping("/tour-categories")
    public ResponseEntity<BaseResponse<List<TourCategoryResponse>>> getPublicTourCategories(
            @RequestParam(defaultValue = "false") boolean withTours) {
        List<TourCategoryResponse> response = withTours
                ? tourCategoryService.getAllWithTours(true)
                : tourCategoryService.getAll(true);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour categories retrieved successfully"));
    }

    @GetMapping("/tour-categories/{id}")
    public ResponseEntity<BaseResponse<TourCategoryResponse>> getPublicTourCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.ok(
                tourCategoryService.getById(id), "Tour category retrieved successfully"));
    }

}
