package com.aerionsoft.application.service.audit;

import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class ActivityAgencyContextSupport {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;

    public ActivityAgencyContextSupport(UserRepository userRepository, BusinessRepository businessRepository) {
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
    }

    public void enrichMetadataFromUserId(Map<String, Object> metadata, Long userId) {
        if (metadata == null || userId == null) {
            return;
        }
        resolveSnapshot(userId).ifPresent(snapshot -> putSnapshot(metadata, snapshot));
    }

    public void enrichMetadataFromBooking(Map<String, Object> metadata, Booking booking) {
        if (metadata == null || booking == null || booking.getCreatedBy() == null) {
            return;
        }
        User owner = booking.getCreatedBy();
        metadata.put("bookingId", booking.getId());
        metadata.put("pnr", booking.getPnr());
        metadata.put("ticketNo", booking.getTicketNo());
        metadata.put("bookingReference", booking.getBookingReference());
        enrichMetadataFromUserId(metadata, owner.getId());
    }

    public Optional<AgencySnapshot> resolveSnapshot(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId).map(this::buildSnapshot);
    }

    public Optional<AgencySnapshot> resolveFromBusinessId(Long businessId) {
        if (businessId == null) {
            return Optional.empty();
        }
        return businessRepository.findById(businessId)
                .flatMap(business -> {
                    if (business.getMotherUser() == null) {
                        return Optional.of(fromBusinessOnly(business, null, null));
                    }
                    return Optional.of(buildSnapshot(business.getMotherUser(), business, null));
                });
    }

    private AgencySnapshot buildSnapshot(User user) {
        User agencyUser = user.getParentUser() != null ? user.getParentUser() : user;
        User owner = user.getParentUser() != null ? user : null;
        BusinessEntity business = businessRepository.findFirstByMotherUser(agencyUser).orElse(null);
        return buildSnapshot(agencyUser, business, owner);
    }

    private AgencySnapshot buildSnapshot(User agencyUser, BusinessEntity business, User owner) {
        String agencyName = business != null && business.getCompanyName() != null && !business.getCompanyName().isBlank()
                ? business.getCompanyName()
                : agencyUser.getFullName();
        String agencyEmail = business != null && business.getCompanyEmail() != null && !business.getCompanyEmail().isBlank()
                ? business.getCompanyEmail()
                : agencyUser.getEmail();
        return new AgencySnapshot(
                business != null ? business.getId() : null,
                agencyUser.getId(),
                agencyName,
                agencyEmail,
                business != null ? business.getCompanyPhone() : agencyUser.getPhoneNumber(),
                agencyUser.getCurrency(),
                owner != null ? owner.getId() : null,
                owner != null ? owner.getFullName() : null,
                owner != null ? owner.getEmail() : null);
    }

    private AgencySnapshot fromBusinessOnly(BusinessEntity business, User agencyUser, User owner) {
        return new AgencySnapshot(
                business.getId(),
                agencyUser != null ? agencyUser.getId() : null,
                business.getCompanyName(),
                business.getCompanyEmail(),
                business.getCompanyPhone(),
                agencyUser != null ? agencyUser.getCurrency() : null,
                owner != null ? owner.getId() : null,
                owner != null ? owner.getFullName() : null,
                owner != null ? owner.getEmail() : null);
    }

    public void putSnapshot(Map<String, Object> metadata, AgencySnapshot snapshot) {
        metadata.put("businessId", snapshot.businessId());
        metadata.put("agencyUserId", snapshot.agencyUserId());
        metadata.put("agencyName", snapshot.agencyName());
        metadata.put("agencyEmail", snapshot.agencyEmail());
        metadata.put("agencyPhone", snapshot.agencyPhone());
        metadata.put("agencyCurrency", snapshot.agencyCurrency());
        if (snapshot.ownerUserId() != null) {
            metadata.put("ownerUserId", snapshot.ownerUserId());
            metadata.put("ownerUserName", snapshot.ownerUserName());
            metadata.put("ownerUserEmail", snapshot.ownerUserEmail());
        }
    }

    public Optional<AgencySnapshot> snapshotFromMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.get("agencyUserId") == null) {
            return Optional.empty();
        }
        return Optional.of(new AgencySnapshot(
                longVal(metadata.get("businessId")),
                longVal(metadata.get("agencyUserId")),
                stringVal(metadata.get("agencyName")),
                stringVal(metadata.get("agencyEmail")),
                stringVal(metadata.get("agencyPhone")),
                stringVal(metadata.get("agencyCurrency")),
                longVal(metadata.get("ownerUserId")),
                stringVal(metadata.get("ownerUserName")),
                stringVal(metadata.get("ownerUserEmail"))));
    }

    private static Long longVal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String stringVal(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    public record AgencySnapshot(
            Long businessId,
            Long agencyUserId,
            String agencyName,
            String agencyEmail,
            String agencyPhone,
            String agencyCurrency,
            Long ownerUserId,
            String ownerUserName,
            String ownerUserEmail) {
    }
}
