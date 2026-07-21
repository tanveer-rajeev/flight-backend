package com.aerionsoft.application.service.booking;

import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.traveller.TravellerRequest;
import com.aerionsoft.application.dto.traveller.TravellerResponse;
import com.aerionsoft.application.entity.Booking.Traveller;
import com.aerionsoft.application.repository.booking.TravellerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class TravellerService {

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    @Autowired
    private TravellerRepository travellerRepo;

    public TravellerResponse createTraveller(TravellerRequest req, Long userID) {
//        /        @Column(name = "visaOrNidImageURL")
//        private String visaOrNidImageURL;
//
//        @Column(name = "passportImageUrl")
//        private String passportImageUrl;
        Traveller.TravellerBuilder travellerBuilder = Traveller.builder().title(req.getTitle()).firstName(req.getFirstName()).
                lastName(req.getLastName()).mobile(req.getMobile())
                .mobileCountryCode(req.getMobileCountryCode()).email(req.getEmail()).
                gender(req.getGender()).countryName(req.getCountryName()).
                countryCode(req.getCountryCode()).cityName(req.getCityName()).cityCode(req.getCityCode()).
                mealCode(req.getMealCode()).addressLine1(req.getAddressLine1()).addressLine2(req.getAddressLine2()).visaOrNidImageURL(req.getVisaOrNidImageURL()).
                passportImageUrl(req.getPassportImageUrl()).
                nationality(req.getCountryName()).passportNo(req.getPassportNo()).createdBy(userID);

        Optional.ofNullable(req.getDob()).filter(s -> !s.isEmpty()).map(s -> LocalDate.parse(s, dateFormatter)).ifPresent(travellerBuilder::dob);

        Optional.ofNullable(req.getPassportIssueDate()).filter(s -> !s.isEmpty()).map(s -> LocalDate.parse(s, dateFormatter)).ifPresent(travellerBuilder::passportIssueDate);

        Optional.ofNullable(req.getPassportExpiryDate()).filter(s -> !s.isEmpty()).map(s -> LocalDate.parse(s, dateFormatter)).ifPresent(travellerBuilder::passportExpiryDate);



        Traveller traveller = travellerBuilder.build();

        travellerRepo.save(traveller);
        return mapToResponse(traveller);
    }

    public TravellerResponse updateTraveller(Long id, TravellerRequest req) {
        Traveller traveller = travellerRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Traveller"));
        traveller.setTitle(req.getTitle());
        traveller.setFirstName(req.getFirstName());
        traveller.setLastName(req.getLastName());
        traveller.setMobile(req.getMobile());
        traveller.setMobileCountryCode(req.getMobileCountryCode());
        traveller.setEmail(req.getEmail());
        traveller.setGender(req.getGender());
        traveller.setDob(LocalDate.parse(req.getDob(), dateFormatter));
        traveller.setPassportNo(req.getPassportNo());
        traveller.setPassportIssueDate(LocalDate.parse(req.getPassportIssueDate(), dateFormatter));
        traveller.setPassportExpiryDate(LocalDate.parse(req.getPassportExpiryDate(), dateFormatter));
        traveller.setCountryName(req.getCountryName());
        traveller.setCountryCode(req.getCountryCode());
        traveller.setCityName(req.getCityName());
        traveller.setCityCode(req.getCityCode());
        traveller.setMealCode(req.getMealCode());
        traveller.setAddressLine1(req.getAddressLine1());
        traveller.setAddressLine2(req.getAddressLine2());
        traveller.setNationality(req.getCountryName()); // Update nationality from countryName
        travellerRepo.save(traveller);
        return mapToResponse(traveller);
    }

    public List<TravellerResponse> getTravellersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<Traveller> travellers = travellerRepo.findAllById(ids);
        return travellers.stream().map(this::mapToResponse).collect(java.util.stream.Collectors.toList());
    }

    public TravellerResponse getTravellerById(Long id) {
        return travellerRepo.findById(id).map(this::mapToResponse).orElseThrow(() -> new ResourceNotFoundException("Traveller", id));
    }

    public TravellerResponse mapToResponse(Traveller t) {
        return new TravellerResponse(t.getId(), t.getTitle(), t.getFirstName(), t.getLastName(), t.getMobile(), t.getMobileCountryCode(), t.getEmail(), t.getGender(), t.getDob(), t.getPassportNo(), t.getPassportIssueDate(), t.getPassportExpiryDate(), t.getCountryName(), t.getCountryCode(), t.getCityName(), t.getCityCode(), t.getMealCode(), t.getNationality(), t.getAddressLine1(), t.getAddressLine2()
        , t.getVisaOrNidImageURL(), t.getPassportImageUrl(), null);
    }

    public Page<TravellerResponse> searchTravellers(String query, int page, int size,
                                                    Long createdBy, String provider) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        boolean hasQuery = StringUtils.hasText(query);
        boolean filterByUser = "user".equalsIgnoreCase(provider) && createdBy != null;

        Page<Traveller> travellerPage;

        if (hasQuery && filterByUser) {
            travellerPage = travellerRepo.searchByQueryAndCreatedBy(query, createdBy, pageable);

        } else if (hasQuery) {
            travellerPage = travellerRepo.searchByQuery(query, pageable);

        } else if (filterByUser) {
            travellerPage = travellerRepo.findByCreatedBy(createdBy, pageable);

        } else {
            travellerPage = travellerRepo.findAll(pageable);
        }

        return travellerPage.map(this::mapToResponse);
    }

    public List<TravellerResponse> getTravellerByCreatedBy(Long createdBy) {
        List<Traveller> traveller = travellerRepo.findByCreatedBy(createdBy);
        if (traveller.isEmpty()) {
            return null;
        }
        return traveller.stream().map(this::mapToResponse).toList();
    }

    public Integer getTravellerByIds(Collection<Long> travellerIds) {
        if (travellerIds == null || travellerIds.isEmpty()) {
            return 0;
        }

        return travellerRepo.countByIdIn(travellerIds);
    }

    public Page<TravellerResponse> searchTravellersWithFilters(
            Long createdBy,
            String firstName,
            String lastName,
            String mobile,
            String passportNo,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        // Convert empty strings to null for the query
        String firstNameFilter = StringUtils.hasText(firstName) ? firstName : null;
        String lastNameFilter = StringUtils.hasText(lastName) ? lastName : null;
        String mobileFilter = StringUtils.hasText(mobile) ? mobile : null;
        String passportNoFilter = StringUtils.hasText(passportNo) ? passportNo : null;

        Page<Traveller> travellerPage = travellerRepo.searchWithFilters(
                createdBy,
                firstNameFilter,
                lastNameFilter,
                mobileFilter,
                passportNoFilter,
                pageable
        );

        return travellerPage.map(this::mapToResponse);
    }

    public TravellerResponse createOrUpdateTraveller(TravellerRequest req, Long userId) {
        java.util.Optional<Traveller> existing = findExistingTraveller(req, userId);
        Traveller traveller;

        if (existing.isPresent()) {
            traveller = existing.get();

            traveller.setTitle(req.getTitle());
            traveller.setFirstName(req.getFirstName());
            traveller.setLastName(req.getLastName());
            traveller.setMobile(req.getMobile());
            traveller.setMobileCountryCode(req.getMobileCountryCode());
            traveller.setEmail(req.getEmail());
            traveller.setGender(req.getGender());
            traveller.setCountryName(req.getCountryName());
            traveller.setCountryCode(req.getCountryCode());
            traveller.setCityName(req.getCityName());
            traveller.setCityCode(req.getCityCode());
            traveller.setMealCode(req.getMealCode());
            traveller.setAddressLine1(req.getAddressLine1());
            traveller.setAddressLine2(req.getAddressLine2());
            traveller.setNationality(req.getCountryName());
            traveller.setPassportNo(req.getPassportNo());
            traveller.setPassportImageUrl(req.getPassportImageUrl());
            traveller.setVisaOrNidImageURL(req.getVisaOrNidImageURL());

            Optional.ofNullable(req.getDob()).filter(s -> !s.isEmpty()).map(s -> LocalDate.parse(s, dateFormatter)).ifPresent(traveller::setDob);
            Optional.ofNullable(req.getPassportIssueDate()).filter(s -> !s.isEmpty()).map(s -> LocalDate.parse(s, dateFormatter)).ifPresent(traveller::setPassportIssueDate);
            Optional.ofNullable(req.getPassportExpiryDate()).filter(s -> !s.isEmpty()).map(s -> LocalDate.parse(s, dateFormatter)).ifPresent(traveller::setPassportExpiryDate);
        } else {
            traveller = buildTravellerEntity(req, Traveller.builder().createdBy(userId)).build();
        }

        travellerRepo.save(traveller);
        return mapToResponse(traveller);
    }

    private Optional<Traveller> findExistingTraveller(TravellerRequest req, Long userId) {
        if (req.getPassportNo() != null && !req.getPassportNo().isBlank()) {
            Optional<Traveller> byPassport = travellerRepo.findFirstByCreatedByAndPassportNoIgnoreCase(userId, req.getPassportNo().trim());
            if (byPassport.isPresent()) {
                return byPassport;
            }
        }

        if (req.getDob() != null && !req.getDob().isBlank()) {
            LocalDate dob = LocalDate.parse(req.getDob(), dateFormatter);
            return travellerRepo.findFirstByCreatedByAndFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDob(
                    userId,
                    req.getFirstName(),
                    req.getLastName(),
                    dob
            );
        }

        return Optional.empty();
    }

    private Traveller.TravellerBuilder buildTravellerEntity(TravellerRequest req, Traveller.TravellerBuilder travellerBuilder) {
        travellerBuilder.title(req.getTitle()).firstName(req.getFirstName())
                .lastName(req.getLastName()).mobile(req.getMobile())
                .mobileCountryCode(req.getMobileCountryCode()).email(req.getEmail())
                .gender(req.getGender()).countryName(req.getCountryName())
                .countryCode(req.getCountryCode()).cityName(req.getCityName()).cityCode(req.getCityCode())
                .mealCode(req.getMealCode()).addressLine1(req.getAddressLine1()).addressLine2(req.getAddressLine2())
                .visaOrNidImageURL(req.getVisaOrNidImageURL())
                .passportImageUrl(req.getPassportImageUrl())
                .nationality(req.getCountryName()).passportNo(req.getPassportNo());

        Optional.ofNullable(req.getDob()).filter(s -> !s.isEmpty()).map(s -> LocalDate.parse(s, dateFormatter)).ifPresent(travellerBuilder::dob);
        Optional.ofNullable(req.getPassportIssueDate()).filter(s -> !s.isEmpty()).map(s -> LocalDate.parse(s, dateFormatter)).ifPresent(travellerBuilder::passportIssueDate);
        Optional.ofNullable(req.getPassportExpiryDate()).filter(s -> !s.isEmpty()).map(s -> LocalDate.parse(s, dateFormatter)).ifPresent(travellerBuilder::passportExpiryDate);
        return travellerBuilder;
    }

}