package com.aerionsoft.application.service.business;
import com.aerionsoft.application.service.common.CurrencyService;
import com.aerionsoft.application.service.common.AgentIdGenerator;
import com.aerionsoft.application.service.access.RoleService;

import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.business.BusinessDto;
import com.aerionsoft.application.dto.business.BusinessRequest;
import com.aerionsoft.application.dto.business.PublicAgencyRequest;
import com.aerionsoft.application.dto.UpdateBusinessRequest;
import com.aerionsoft.application.dto.UpdateBusinessStatusRequest;
import com.aerionsoft.application.dto.client.user.UserDto;
import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.rolePermission.Role;
import com.aerionsoft.application.entity.rolePermission.RoleAssignment;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.enums.business.BusinessStatus;
import com.aerionsoft.application.repository.business.BusinessProviderRepository;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.notification.NotificationPreferenceRepository;
import com.aerionsoft.application.repository.notification.NotificationRepository;
import com.aerionsoft.application.repository.user.LoginHistoryRepository;
import com.aerionsoft.application.repository.user.RefreshTokenRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.wallet.BalanceChangeHistoryRepository;
import com.aerionsoft.application.repository.wallet.CreditRequestRepository;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import com.aerionsoft.application.entity.CreditLimitHistory;
import com.aerionsoft.application.enums.wallet.CreditLimitStatus;
import com.aerionsoft.application.repository.wallet.CreditLimitHistoryRepository;
import com.aerionsoft.application.repository.wallet.TransactionRepository;
import com.aerionsoft.application.repository.access.RoleAssignmentRepository;
import com.aerionsoft.application.util.EmailUtils;
import com.aerionsoft.application.util.TimestampMapper;
import com.aerionsoft.application.util.UserDateTimeUtil;
import com.aerionsoft.application.repository.access.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BusinessServiceImpl implements BusinessService {

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final BusinessSalesPersonService businessSalesPersonService;
    private final TransactionRepository transactionRepository;
    private final CreditLimitHistoryRepository creditLimitHistoryRepository;
    private final TimestampMapper timestampMapper;

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private RoleAssignmentRepository roleAssignmentRepository;
    @Autowired
    private AgentIdGenerator agentIdGenerator;
    @Autowired
    private CurrencyService currencyService;
    @Autowired
    private WalletDepositRepository walletDepositRepository;
    @Autowired
    private BalanceChangeHistoryRepository balanceChangeHistoryRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;
    @Autowired
    private LoginHistoryRepository loginHistoryRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private CreditRequestRepository creditRequestRepository;
    @Autowired
    private BusinessProviderRepository businessProviderRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public BusinessDto createBusiness(BusinessRequest request) {

        User motherUser = userRepository.findById(request.getMotherUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        // Check if user is already a mother user of another business
        if (businessRepository.existsByMotherUser(motherUser)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "This user is already associated with another business as mother user");
        }

        motherUser.setAgency(true);
        userRepository.save(motherUser);

        BusinessEntity business = new BusinessEntity();
        business.setCompanyName(request.getCompanyName());
        business.setCompanyEmail(request.getCompanyEmail());
        business.setCompanyAddress(request.getCompanyAddress());
        business.setCompanyPhone(request.getCompanyPhone());
        business.setMotherUser(motherUser);
        business.setStatus(BusinessStatus.APPROVED);
        business.setCompanyLogo(request.getCompanyLogo());
        business.setCompanyLicence(request.getCompanyLicence());
        business.setCivilAviationCertNumber(request.getCivilAviationCertNumber());
        business.setCivilAviationCertExpiryDate(request.getCivilAviationCertExpiryDate());
        business.setAddressProof(request.getAddressProof());
        business.setAttachment(request.getAttachment());
        business.setRepresentativeName(request.getRepresentativeName());
        business.setRepresentativeMobile(request.getRepresentativeMobile());
        business.setRepresentativeEmail(request.getRepresentativeEmail());
        business.setRepresentativePosition(request.getRepresentativePosition());
        business.setDigitalSignature(request.getDigitalSignature());

        return mapToDto(businessRepository.save(business));
    }

    @Override
    public BusinessDto requestBusiness(BusinessRequest request) {
        User requester = userRepository.findById(request.getMotherUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        // Check if user is already a mother user of another business
        if (businessRepository.existsByMotherUser(requester)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "This user is already associated with another business as mother user");
        }

        BusinessEntity business = new BusinessEntity();
        business.setCompanyName(request.getCompanyName());
        business.setCompanyEmail(request.getCompanyEmail());
        business.setCompanyAddress(request.getCompanyAddress());
        business.setCompanyPhone(request.getCompanyPhone());
        business.setMotherUser(requester);
        business.setStatus(BusinessStatus.PENDING);
        business.setCompanyLogo(request.getCompanyLogo());
        business.setCompanyLicence(request.getCompanyLicence());
        business.setCivilAviationCertNumber(request.getCivilAviationCertNumber());
        business.setCivilAviationCertExpiryDate(request.getCivilAviationCertExpiryDate());
        business.setAddressProof(request.getAddressProof());
        business.setAttachment(request.getAttachment());
        business.setRepresentativeName(request.getRepresentativeName());
        business.setRepresentativeMobile(request.getRepresentativeMobile());
        business.setRepresentativeEmail(request.getRepresentativeEmail());
        business.setRepresentativePosition(request.getRepresentativePosition());
        business.setDigitalSignature(request.getDigitalSignature());

        return mapToDto(businessRepository.save(business));
    }

    @Override
    @Transactional
    public BusinessDto createPublicAgency(PublicAgencyRequest request) {
        String email = EmailUtils.normalize(request.getRepresentativeEmail());

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "User with this email already exists");
        }

        Set<String> defaultRoles = new HashSet<>();
        defaultRoles.add("USER");

        User motherUser = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getRepresentativeName())
                .phoneNumber(request.getRepresentativeMobile())
                .isVerified(true)
                .isActive(true)
                .isAgency(false)
                .createdAt(UserDateTimeUtil.now())
                .currency(Currency.fromCode(request.getCurrency()))
                .balance(0.0)
                .roles(defaultRoles)
                .ticketPreview("compact")
                .build();
        motherUser = userRepository.save(motherUser);

        BusinessEntity business = new BusinessEntity();
        business.setCompanyName(request.getCompanyName());
        business.setCompanyEmail(request.getCompanyEmail() != null ? request.getCompanyEmail() : email);
        business.setCompanyAddress(request.getCompanyAddress());
        business.setCompanyPhone(request.getCompanyPhone() != null
                ? request.getCompanyPhone()
                : request.getRepresentativeMobile());
        business.setMotherUser(motherUser);
        business.setStatus(BusinessStatus.PENDING);
        business.setCompanyLogo(request.getCompanyLogo());
        business.setCompanyLicence(request.getCompanyLicence());
        business.setCivilAviationCertNumber(request.getCivilAviationCertNumber());
        business.setCivilAviationCertExpiryDate(request.getCivilAviationCertExpiryDate());
        business.setAddressProof(request.getAddressProof());
        business.setAttachment(request.getAttachment());
        business.setRepresentativeName(request.getRepresentativeName());
        business.setRepresentativeMobile(request.getRepresentativeMobile());
        business.setRepresentativeEmail(email);
        business.setRepresentativePosition(request.getRepresentativePosition());
        business.setDigitalSignature(request.getDigitalSignature());

        return mapToDto(businessRepository.save(business));
    }

    @Override
    public BusinessDto approveBusiness(Long businessId) {
        BusinessEntity business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business"));
        business.setStatus(BusinessStatus.APPROVED);

        User motherUser = business.getMotherUser();
        String code = agentIdGenerator.generate(motherUser.getCurrency(), null);
        motherUser.setCode(code);
        motherUser.setAgency(true);
        motherUser.setPhoneNumber(business.getCompanyPhone());
        motherUser.setTicketPreview("compact");
        userRepository.save(motherUser);

        // assign role as agency
        Role role = roleRepository.findBySlug("agency").orElseThrow(() -> new ResourceNotFoundException("Role agency"));

        RoleAssignment roleAssignment = RoleAssignment.builder()
                .entityId(motherUser.getId())
                .entityType("USER")
                .role(role)
                .build();

        roleAssignmentRepository.save(roleAssignment);

        return mapToDto(businessRepository.save(business));
    }

    @Override
    @Transactional
    public BusinessDto updateBusinessStatus(Long businessId, UpdateBusinessStatusRequest request) {
        BusinessEntity business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business"));

        BusinessStatus oldStatus = business.getStatus();
        BusinessStatus newStatus = request.getStatus();

        business.setStatus(newStatus);

        // If approving the business, set up the mother user as agency
        if (newStatus == BusinessStatus.APPROVED && oldStatus != BusinessStatus.APPROVED) {
            User motherUser = business.getMotherUser();
            motherUser.setCode(agentIdGenerator.generate(motherUser.getCurrency(), null));
            motherUser.setAgency(true);
            motherUser.setTicketPreview("compact");
            userRepository.save(motherUser);

            // Assign role as agency
            Role role = roleRepository.findBySlug("agency")
                    .orElseThrow(() -> new ResourceNotFoundException("Role agency"));

            // Check if role assignment already exists
            Optional<RoleAssignment> existingAssignment = roleAssignmentRepository
                    .findByEntityIdAndEntityTypeAndRole(motherUser.getId(), "USER", role);

            if (existingAssignment.isEmpty()) {
                RoleAssignment roleAssignment = RoleAssignment.builder()
                        .entityId(motherUser.getId())
                        .entityType("USER")
                        .role(role)
                        .build();
                roleAssignmentRepository.save(roleAssignment);
            }
        }

        // If rejecting or setting to pending from approved, optionally revoke agency status
        if (oldStatus == BusinessStatus.APPROVED && newStatus != BusinessStatus.APPROVED) {
            User motherUser = business.getMotherUser();
            motherUser.setAgency(false);
            userRepository.save(motherUser);

            // Optionally remove agency role
            Role role = roleRepository.findBySlug("agency")
                    .orElseThrow(() -> new ResourceNotFoundException("Role agency"));
            roleAssignmentRepository.deleteByEntityIdAndEntityTypeAndRole(
                    motherUser.getId(), "USER", role);
        }

        return mapToDto(businessRepository.save(business));
    }

    @Override
    public BusinessDto assignMotherUser(Long businessId, Long userId) {
        BusinessEntity business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business"));
        User motherUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        // Check if user is already a mother user of another business
        if (businessRepository.existsByMotherUser(motherUser)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "This user is already associated with another business as mother user");
        }

        business.setMotherUser(motherUser);
        return mapToDto(businessRepository.save(business));
    }

    @Override
    public BusinessDto updateBusiness(Long businessId, UpdateBusinessRequest request) {
        BusinessEntity business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business"));

        business.setCompanyName(request.getCompanyName());
        business.setCompanyEmail(request.getCompanyEmail());
        business.setCompanyAddress(request.getCompanyAddress());
        business.setCompanyPhone(request.getCompanyPhone());
        business.setCompanyLogo(request.getCompanyLogo());
        business.setCompanyLicence(request.getCompanyLicence());
        business.setCivilAviationCertNumber(request.getCivilAviationCertNumber());
        business.setCivilAviationCertExpiryDate(request.getCivilAviationCertExpiryDate());
        business.setAddressProof(request.getAddressProof());
        business.setAttachment(request.getAttachment());
        business.setRepresentativeName(request.getRepresentativeName());
        business.setRepresentativeMobile(request.getRepresentativeMobile());
        business.setRepresentativeEmail(request.getRepresentativeEmail());
        business.setRepresentativePosition(request.getRepresentativePosition());
        business.setDigitalSignature(request.getDigitalSignature());

        // Capture previous limit before updating
        BigDecimal previousLimit = business.getCreditLimit();
        BigDecimal newLimit = request.getCreditLimit();

        business.setCreditLimit(newLimit);

        // Track credit limit change in history
        if (newLimit != null && (previousLimit == null || previousLimit.compareTo(newLimit) != 0)) {
            CreditLimitStatus status;
            if (previousLimit == null || newLimit.compareTo(previousLimit) > 0) {
                status = CreditLimitStatus.CREDIT;
            } else {
                status = CreditLimitStatus.DEBIT;
            }

            CreditLimitHistory history = CreditLimitHistory.builder()
                    .businessId(businessId)
                    .amount(newLimit)
                    .balanceBefore(previousLimit != null ? previousLimit : BigDecimal.ZERO)
                    .balanceAfter(newLimit)
                    .cause("Credit limit updated from " +
                           (previousLimit != null ? previousLimit : "N/A") +
                           " to " + newLimit)
                    .status(status)
                    .build();

            creditLimitHistoryRepository.save(history);
        }

        return mapToDto(businessRepository.save(business));
        
        
    }

    @Override
    public BusinessDto getBusinessById(Long businessId) {
        BusinessEntity business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business"));
        return mapToDtoWithSalesPersons(business);
    }

    private BusinessDto mapToDtoWithSalesPersons(BusinessEntity business) {
        BusinessDto dto = mapToDto(business);
        dto.setSalesPersons(businessSalesPersonService.getSalesPersons(business.getId()));
        return dto;
    }

    private BusinessDto mapToDto(BusinessEntity business) {
        BusinessDto dto = new BusinessDto();
        dto.setId(business.getId());
        dto.setCompanyName(business.getCompanyName());
        dto.setCompanyEmail(business.getCompanyEmail());
        dto.setCompanyAddress(business.getCompanyAddress());
        dto.setCompanyPhone(business.getCompanyPhone());
        dto.setCompanyLogo(business.getCompanyLogo());
        dto.setCompanyLicence(business.getCompanyLicence());
        dto.setCivilAviationCertNumber(business.getCivilAviationCertNumber());
        dto.setCivilAviationCertExpiryDate(business.getCivilAviationCertExpiryDate());
        dto.setAddressProof(business.getAddressProof());
        dto.setAttachment(business.getAttachment());
        dto.setRepresentativeName(business.getRepresentativeName());
        dto.setRepresentativeMobile(business.getRepresentativeMobile());
        dto.setRepresentativeEmail(business.getRepresentativeEmail());
        dto.setRepresentativePosition(business.getRepresentativePosition());
        dto.setBalance(business.getMotherUser() != null && business.getMotherUser().getBalance() != null
                ? BigDecimal.valueOf(business.getMotherUser().getBalance()) : BigDecimal.ZERO);
        dto.setDigitalSignature(business.getDigitalSignature());
        dto.setCreditLimit(business.getCreditLimit());


        if (business.getMotherUser() != null) {
            dto.setMotherUserId(business.getMotherUser().getId());
            dto.setMotherUserFullName(business.getMotherUser().getFullName());
            dto.setAgencyCode(business.getMotherUser().getCode());
            Role role = roleService.getRoleByUserId(business.getMotherUser().getId());
            if (role != null) {
                dto.setRole(role.getName());
            }

            // Set mother user's current balance
            dto.setMotherCurrentBalance(business.getMotherUser().getBalance());

            // Calculate total credit and debit from transactions
            Long motherUserId = business.getMotherUser().getId();

            List<Transaction> allTransactions = transactionRepository.findAll().stream()
                    .filter(txn -> txn.getUserId() != null && txn.getUserId().equals(motherUserId) && txn.isActive())
                    .toList();

            double totalCredit = allTransactions.stream()
                    .filter(txn -> isDepositType(txn.getType()))
                    .mapToDouble(txn -> txn.getAmount() != null ? txn.getAmount() : 0.0)
                    .sum();


            double totalDebit = allTransactions.stream()
                    .filter(txn -> isDeductionType(txn.getType()))
                    .mapToDouble(txn -> txn.getAmount() != null ? txn.getAmount() : 0.0)
                    .sum();


            dto.setMotherTotalCreditInUC(totalCredit);
            dto.setMotherTotalDebitInUC(totalDebit);
            dto.setMotherTotalCredit(totalCredit);
            dto.setMotherTotalDebit(totalDebit);
            String targetCurrency = business.getMotherUser().getCurrency();

            if (targetCurrency == null || targetCurrency.isBlank()) {
                targetCurrency = "USD";
            }

//            Double exchangeRate = Double.valueOf(currencyService.getExchangeRate("USD", targetCurrency, "OTHERS"));

            if (business.getMotherUser().getBalance() == null) {
                business.getMotherUser().setBalance(0.0);
            } else {
                dto.setMotherCurrentBalanceInUC(business.getMotherUser().getBalance());

//                dto.setMotherCurrentBalanceInUC(exchangeRate * business.getMotherUser().getBalance());
            }
            dto.setMotherUserCurrency(targetCurrency);
        }
        dto.setStatus(business.getStatus());
        return dto;
    }


    private double safeParseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }


    private boolean isDepositType(String type) {
        if (type == null) return false;
        return type.equalsIgnoreCase("DEPOSIT") || type.equalsIgnoreCase("BANK_DEPOSIT") ||
                type.equalsIgnoreCase("REFUND") || type.equalsIgnoreCase("CASH") ||
                type.equalsIgnoreCase("CREDIT");

    }
    private boolean isDeductionType(String type) {
        if (type == null) return false;
        return type.equalsIgnoreCase("BOOKING_DEDUCTION") ||
                type.equalsIgnoreCase("DEDUCTION") ||
                type.equalsIgnoreCase("WITHDRAWAL") ||  type.equalsIgnoreCase("ADMIN_CHARGE") ||
                type.equalsIgnoreCase("PURCHASE");
    }

    public List<UserDto> getUsersOfBusiness(Long businessId) {
        BusinessEntity business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business"));

        List<User> users = userRepository.findByBusiness(business);

        User motherUser = business.getMotherUser();
        users.add(motherUser);

        return users.stream().map(this::mapToResponseWithRole).toList();
    }

    @Override
    public Page<BusinessDto> getAllBusinesses(String currencyCode, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        String currency = (currencyCode == null || currencyCode.isBlank()) ? null : currencyCode.toUpperCase();

        Page<BusinessEntity> businesses;

        if (query != null && !query.isBlank()) {
            // Use search query if provided
            businesses = businessRepository.findByMotherUserCurrencyAndQuery(currency, query, pageable);
        } else {
            // Use existing method if no search query
            businesses = businessRepository.findByMotherUserCurrency(currency, pageable);
        }

        return businesses.map(this::mapToDto);
    }

    @Override
    public BusinessDto getBusinessByUserId(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        // First try to find business directly associated with the user
        return businessRepository.findFirstByMotherUser(user)
                .map(this::mapToDtoWithSalesPersons)
                .orElseGet(() -> {
                    // If user has no parent, there is no fallback business
                    if (user.getParentUser() == null) {
                        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "This user is not associated with any business");
                    }

                    // Fallback to parent user's business
                    return businessRepository.findFirstByMotherUser(user.getParentUser())
                            .map(this::mapToDtoWithSalesPersons)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException("Business for parent user"));
                });
    }


    private UserDto mapToResponse(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setFullName(user.getFullName());
        userDto.setPhoneNumber(user.getPhoneNumber());
        userDto.setImage(user.getImage());
        userDto.setBalance(user.getBalance());
        userDto.setPassportNumber(user.getPassportNumber());
        userDto.setPassportExpiryDate(user.getPassportExpiryDate());
        userDto.setAddress(user.getAddress());
        userDto.setDob(user.getDob());
        userDto.setCreatedAt(timestampMapper.createdAtString(user));

        userDto.setCurrency(user.getCurrency());
        userDto.setNationality(user.getNationality());
        userDto.setAgency(user.isAgency());
        userDto.setCode(user.getCode());
        userDto.setDeleted(user.isDeleted());
        userDto.setTicketPreview(user.getTicketPreview());
        userDto.setInvoicePreview(user.getInvoicePreview());
        userDto.setMoneyReceiptPreview(user.getMoneyReceiptPreview());

        return userDto;
    }

    private UserDto mapToResponseWithRole(User user) {
        UserDto userDto = mapToResponse(user);

        // Add role name (lazy loaded)
        Role roleNames = roleService.getRoleByUserId(user.getId());
        userDto.setRole(roleNames != null ? roleNames.getName() : null);

        return userDto;
    }

    @Override
    @Transactional
    public void deleteRejectedAgency(Long businessId) {
        BusinessEntity business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency", businessId));

        if (business.getStatus() != BusinessStatus.REJECTED) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Only REJECTED agencies can be deleted. Current status: " + business.getStatus());
        }

        // Collect all users belonging to this business (mother + children)
        List<User> allUsers = userRepository.findByBusinessId(businessId);

        // For each user, delete all their related data
        for (User user : allUsers) {
            Long userId = user.getId();

            // Wallet & financial data
            transactionRepository.deleteByUserId(userId);
            walletDepositRepository.deleteByUserId(userId);
            balanceChangeHistoryRepository.deleteByUserId(userId);

            // Notifications
            notificationRepository.deleteByUserId(userId);
            notificationPreferenceRepository.deleteByUserId(userId);

            // Auth / session data
            refreshTokenRepository.deleteByUser(user);
            loginHistoryRepository.deleteByUserId(userId);

            // Role assignment
            roleAssignmentRepository.findByEntityTypeAndEntityId("USER", userId)
                    .ifPresent(roleAssignmentRepository::delete);
        }

        // Business-level data
        creditLimitHistoryRepository.deleteByBusinessId(businessId);
        creditRequestRepository.deleteByBusinessId(businessId);
        businessProviderRepository.deleteByBusinessId(businessId);

        // Soft-delete all users (mark isDeleted=true to respect @Where clause)
        for (User user : allUsers) {
            user.setDeleted(true);
            user.setIsActive(false);
            user.setBusiness(null);
        }
        userRepository.saveAll(allUsers);

        // Delete the business itself
        businessRepository.delete(business);
    }
}
