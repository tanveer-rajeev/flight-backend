package com.aerionsoft.application.service.user;

import com.aerionsoft.application.util.DepositBankMapper;
import com.aerionsoft.application.util.TimestampMapper;
import com.aerionsoft.application.util.UserDateTimeUtil;
import com.aerionsoft.application.service.common.EmailService;
import com.aerionsoft.application.service.wallet.CreditLimitService;
import com.aerionsoft.application.service.wallet.CreditLimitValidatorService;
import com.aerionsoft.application.service.common.CurrencyService;
import com.aerionsoft.application.service.common.AgentIdGenerator;
import com.aerionsoft.application.service.access.RoleService;
import com.aerionsoft.application.service.common.FileStorageService;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.ChildUserRequest;
import com.aerionsoft.application.dto.admin.bank.WalletDepositResponse;
import com.aerionsoft.application.dto.admin.client.AgencyProfileDto;
import com.aerionsoft.application.dto.admin.client.AgencyUserDto;
import com.aerionsoft.application.dto.admin.summery.LastTenAgencies;
import com.aerionsoft.application.dto.admin.summery.LastTenUsers;
import com.aerionsoft.application.dto.client.user.CreateUserRequest;
import com.aerionsoft.application.dto.client.user.UpdateProfileRequest;
import com.aerionsoft.application.dto.client.user.UpdateUserRequest;
import com.aerionsoft.application.dto.client.user.UserDto;
import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.rolePermission.Role;
import com.aerionsoft.application.entity.wallet.BalanceChangeHistory;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.interafces.UserInterface;
import com.aerionsoft.application.repository.spec.OffsetAwareDateSpec;
import com.aerionsoft.application.repository.wallet.BalanceChangeHistoryRepository;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.wallet.TransactionRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import com.aerionsoft.application.util.EmailUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService implements UserInterface {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private WalletDepositRepository depositRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RoleService roleService;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BalanceChangeHistoryRepository balanceChangeHistoryRepository;

    @Autowired
    private AgentIdGenerator agentIdGenerator;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private CreditLimitValidatorService creditLimitValidatorService;

    @Autowired
    private CreditLimitService creditLimitService;

    @Autowired
    private TimestampMapper timestampMapper;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("generalEmailService")
    private EmailService emailService;

    @org.springframework.beans.factory.annotation.Value("${platform.name}")
    private String platformName;

    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }


    public User updateProfile(String email, UpdateProfileRequest req) {
        User user = userRepo.findByEmail(email).orElseThrow();

        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getPhoneNumber() != null) user.setPhoneNumber(req.getPhoneNumber());
        if (req.getBalance() != null) user.setBalance(req.getBalance());
        if (req.getPassportNumber() != null) user.setPassportNumber(req.getPassportNumber());
        if (req.getPassportExpiryDate() != null) user.setPassportExpiryDate(req.getPassportExpiryDate());
        if (req.getAddress() != null) user.setAddress(req.getAddress());
        if (req.getAddress() != null) user.setAddress(req.getAddress());
        if (req.getDob() != null) user.setDob(req.getDob());
        if (req.getNationality() != null) user.setNationality(req.getNationality());
        if (req.getCurrency() != null) user.setCurrency(req.getCurrency());
        user.setVerified(true);

        userRepo.save(user);
        return user;
    }


    public UserDto getProfile(String email) {
        User user = userRepo.findByEmail(email).orElseThrow();
        UserDto userDto = mapToResponse(user);
        // If this is a child user, return parent's balance instead


        if (user.isAgency()) {
            if (user.getParentUser() != null) {
                userDto.setBalance(user.getParentUser().getBalance());
                userDto.setCurrency(user.getParentUser().getCurrency());
                userDto.setTicketPreview(user.getParentUser().getTicketPreview());
                userDto.setInvoicePreview(user.getParentUser().getInvoicePreview());
                userDto.setMoneyReceiptPreview(user.getParentUser().getMoneyReceiptPreview());
                userDto.setChild(true);

                // Recalculate availableBalance with parent's balance
                double parentBalance = user.getParentUser().getBalance() == null ? 0.0 : user.getParentUser().getBalance();
                double creditLimitVal = userDto.getCreditLimit() != null ? userDto.getCreditLimit() : 0.0;
                userDto.setAvailableBalance(CreditLimitValidatorService.calculateAvailableBalance(parentBalance, creditLimitVal));
            }
            boolean isMother = businessRepository.existsByMotherUser(user);
            userDto.setMother(isMother);
        }

        // now findRoleNames and set to userDto
        Role roleNames = roleService.getRoleByUserId(user.getId());
        System.out.println("Role Names: " + (roleNames != null ? roleNames.getName() : "No Role Found"));
        if (roleNames != null) {
            userDto.setRole(roleNames.getName());
        }

        return userDto;
    }

    private UserDto mapToResponse(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setFullName(user.getFullName());
        userDto.setPhoneNumber(user.getPhoneNumber());
        userDto.setImage(user.getImage());
        userDto.setBalance(user.getBalance() == null ? 0.0 : user.getBalance());
        userDto.setPassportNumber(user.getPassportNumber());
        userDto.setPassportExpiryDate(user.getPassportExpiryDate());
        userDto.setAddress(user.getAddress());
        userDto.setDob(user.getDob() != null ? LocalDate.from(user.getDob()) : null);
        userDto.setCreatedAt(timestampMapper.createdAtString(user));
        userDto.setVerified(user.isVerified());
        userDto.setCurrency(user.getCurrency());
        userDto.setNationality(user.getNationality());
        userDto.setCode(user.getCode());
        userDto.setAgency(user.isAgency());
        userDto.setDeleted(user.isDeleted());
        userDto.setTicketPreview(user.getTicketPreview());
        userDto.setInvoicePreview(user.getInvoicePreview());
        userDto.setMoneyReceiptPreview(user.getMoneyReceiptPreview());
        userDto.setParentUserId(user.getParentUser() != null ? user.getParentUser().getId() : null);

        // Set child and mother flags for agency users
        if (user.isAgency()) {
            if (user.getParentUser() != null) {
                userDto.setChild(true);
            }
            boolean isMother = businessRepository.existsByMotherUser(user);
            userDto.setMother(isMother);
        }

        // Set credit limit and available balance
        try {
            User effectiveUser = user.getParentUser() != null ? user.getParentUser() : user;
            double userBalance = effectiveUser.getBalance() == null ? 0.0 : effectiveUser.getBalance();
            java.util.Optional<BusinessEntity> businessOpt = businessRepository.findFirstByMotherUser(effectiveUser);
            if (businessOpt.isPresent()) {
                BusinessEntity business = businessOpt.get();
                double creditLimitValue = 0.0;
                if (business.getCreditLimit() != null) {
                    creditLimitValue = business.getCreditLimit().doubleValue();
                }
                userDto.setCreditLimit(creditLimitValue);
                userDto.setAvailableBalance(CreditLimitValidatorService.calculateAvailableBalance(userBalance, creditLimitValue));
            } else {
                userDto.setCreditLimit(0.0);
                userDto.setAvailableBalance(userBalance);
            }
        } catch (Exception e) {
            double userBalance = user.getBalance() == null ? 0.0 : user.getBalance();
            userDto.setCreditLimit(0.0);
            userDto.setAvailableBalance(userBalance);
        }

        // Get and set role
        Role roleNames = roleService.getRoleByUserId(user.getId());
        if (roleNames != null) {
            userDto.setRole(roleNames.getName());
        }

        return userDto;

    }

//    public Page<AgencyUserDto> getFilteredAgency(
//            int page,
//            int size,
//            String name,
//            Boolean isAgency,
//            Integer status,
//            LocalDate createdDate
//    ) {
//        Specification<User> spec = buildUserSpecification(name, isAgency, status, createdDate);
//        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
//        Page<User> users = userRepo.findAll(spec, pageable);
//        return users.map(this::mapToAgencyResponse);
//    }

    public Page<UserDto> getFilteredUser(
            int page,
            int size,
            String query,
            Boolean isAgency,
            Boolean status,
            LocalDate createdDate
    ) {
        Specification<User> spec = buildUserSpecification(query, isAgency, status, createdDate);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> users = userRepo.findAll(spec, pageable);
        return users.map(this::mapToResponse);
    }


    private Specification<User> buildUserSpecification(String query, Boolean isAgency, Boolean status, LocalDate createdDate) {
        Specification<User> spec = (root, query1, cb) -> cb.conjunction();

        // Search across name, email, and phone
        if (query != null && !query.isEmpty()) {
            spec = spec.and((root, query1, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), "%" + query.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("email")), "%" + query.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("phoneNumber")), "%" + query.toLowerCase() + "%")
            ));
        }

        if (isAgency != null) {
            spec = spec.and((root, query1, cb) ->
                    cb.equal(root.get("isAgency"), isAgency));
        }

        if (status != null) {
            spec = spec.and((root, query1, cb) ->
                    cb.equal(root.get("isVerified"), status));
        }

        if (createdDate != null) {
            Specification<User> dateSpec = OffsetAwareDateSpec.createdAtOnUserDate(
                    createdDate, "createdAt", "createdTimeOffset");
            if (dateSpec != null) {
                spec = spec.and(dateSpec);
            }
        }

        return spec;
    }


    private AgencyUserDto mapToAgencyResponse(User user) {
        if (user == null) return null;

        AgencyUserDto agencyUserDto = new AgencyUserDto();
        agencyUserDto.setId(user.getId());
        agencyUserDto.setFullName(user.getFullName());
        agencyUserDto.setEmail(user.getEmail());
        agencyUserDto.setPhoneNumber(user.getPhoneNumber());
        agencyUserDto.setBalance(user.getBalance() == null ? 0.0 : user.getBalance());
        agencyUserDto.setStatus(user.getIsActive() ? 1 : 0);
        agencyUserDto.setCreatedAt(timestampMapper.createdAtString(user));
        agencyUserDto.setAgency(user.isAgency());
        agencyUserDto.setCode(user.getCode());
        if (user.getCode() == null || user.getCode().length() != 6) {
            String sixDigit = String.format("%06d", Math.abs(user.getId() != null ? user.getId() : 0L));
            user.setCode(sixDigit);
            userRepo.save(user);
        }
        agencyUserDto.setCode(user.getCode());
        agencyUserDto.setAgentCode(user.getCode());
        return agencyUserDto;
    }



    public String uploadProfileImage(String email, MultipartFile file) throws IOException {
        User user = userRepo.findByEmail(email).orElseThrow();
        String filename = fileStorageService.saveFile(file, email);
        user.setImage(filename);
        userRepo.save(user);
        return filename;
    }

    public Long getUserIdByEmail(String email) {
        User user = userRepo.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User"));
        return user.getId();
    }

    public UserDto getUserById(Long userId) {
        return mapToResponse(Objects.requireNonNull(userRepo.findById(userId).orElse(null)));
    }

    public User getUser(Long userId) {
        return Objects.requireNonNull(userRepo.findById(userId).orElse(null));
    }

    @Override
    public String countOfAgencies() {
        Long count = userRepo.countByIsAgency(true);
        return String.valueOf(count);
    }

    @Override
    public String countOfUsers() {
        Long count = userRepo.countByIsAgency(false);
        return String.valueOf(count);
    }

    @Override
    public String countOfTodayAgencies() {
        return String.valueOf(countUsersCreatedOnUserDate(true));
    }

    @Override
    public String countOfTodayUsers() {
        return String.valueOf(countUsersCreatedOnUserDate(false));
    }

    private long countUsersCreatedOnUserDate(boolean isAgency) {
        LocalDate today = UserDateTimeUtil.now().toLocalDate();
        Specification<User> spec = (root, query, cb) -> cb.equal(root.get("isAgency"), isAgency);
        Specification<User> dateSpec = OffsetAwareDateSpec.createdAtOnUserDate(
                today, "createdAt", "createdTimeOffset");
        if (dateSpec != null) {
            spec = spec.and(dateSpec);
        }
        return userRepo.count(spec);
    }


    public List<LastTenAgencies> getLastTenAgencies() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<User> agencies = userRepo.findByIsAgency(true, pageable);
        return agencies.stream()
                .filter(User::isAgency)
                .map(u -> new LastTenAgencies(
                        timestampMapper.createdAtString(u),
                        String.valueOf(u.getId()),
                        String.valueOf(u.getBalance()),
                        u.getIsActive() ? "Active" : "Inactive",
                        u.getFullName(),
                        u.getEmail(),
                        u.getPhoneNumber(),
                        u.getCode()
                ))
                .collect(Collectors.toList());
    }

    public List<LastTenUsers> getLastTenUsers() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<User> users = userRepo.findByIsAgency(false, pageable);
        return users.stream()
                .filter(user -> !user.isAgency())
                .map(u -> new LastTenUsers(
                        timestampMapper.createdAtString(u),
                        String.valueOf(u.getId()),
                        u.getFullName(),
                        u.getEmail(),
                        u.getPhoneNumber(),
                        u.getIsActive() ? "Active" : "Inactive"
                ))
                .collect(Collectors.toList());
    }


    public void updateAgency(Long id, AgencyProfileDto agencyUpdateReq) {
        User user = userRepo.findByIdAndIsAgency(id, true);
        if (user == null) {
            throw new ResourceNotFoundException("Agency");
        }
        if (agencyUpdateReq.getFullName() != null) user.setFullName(agencyUpdateReq.getFullName());
        if (agencyUpdateReq.getPhoneNumber() != null) user.setPhoneNumber(agencyUpdateReq.getPhoneNumber());
        if (agencyUpdateReq.getDob() != null) user.setDob(agencyUpdateReq.getDob());
        if (agencyUpdateReq.getAddress() != null) user.setAddress(agencyUpdateReq.getAddress());
        if (agencyUpdateReq.getPassportNumber() != null) user.setPassportNumber(agencyUpdateReq.getPassportNumber());
        if (agencyUpdateReq.getPassportExpiryDate() != null)
            user.setPassportExpiryDate(String.valueOf(agencyUpdateReq.getPassportExpiryDate()));
        if (agencyUpdateReq.getNationality() != null) user.setNationality(agencyUpdateReq.getNationality());
        if (agencyUpdateReq.getCurrency() != null) user.setCurrency(agencyUpdateReq.getCurrency());
        userRepo.save(user);
    }


    public List<WalletDepositResponse> getAllDepositsByAgency(Long agencyId) {

        return depositRepo.findByUserId(agencyId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public WalletDepositResponse mapToResponse(WalletDeposit d) {

        return WalletDepositResponse.builder()
                .id(d.getId())
                .reference(d.getReference())
                .type(d.getType())
                .status(d.getStatus())
                .amount(d.getAmount())
                .exchangeRate(d.getExchangeRate())
                .remarks(d.getRemarks())
                .attachment(d.getAttachment())
                .chequeNo(d.getChequeNo())
                .chequeBank(d.getChequeBank())
                .chequeIssueDate(d.getChequeIssueDate())
                .depositBank(DepositBankMapper.resolve(d))
                .createdAt(timestampMapper.createdAt(d))
                .approvedAt(timestampMapper.toRequestUserTime(d.getApprovedAt(), d.getCreatedTimeOffset()))
                .approvedBy(String.valueOf(d.getApprovedBy()))
                .createdBy(String.valueOf(d.getUserId()))
                .build();
    }


    public Long createUser(CreateUserRequest request, Boolean isAgency) {
        String email = EmailUtils.normalize(request.getEmail());

        if (userRepo.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "User with this email already exists");
        }

        User user = new User();
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEmail(email);
        user.setFullName(request.getFullName());
        user.setAddress(request.getAddress());
        user.setCurrency(request.getCurrency());
        user.setBalance(request.getBalance());
        user.setPassportNumber(request.getPassportNumber());

        // Convert passportExpiryDate String to LocalDate if needed
        if (request.getPassportExpiryDate() != null && !request.getPassportExpiryDate().isEmpty()) {
            user.setPassportExpiryDate(String.valueOf(LocalDate.parse(request.getPassportExpiryDate())));
        }

        user.setVerified(true);
        user.setAgency(isAgency);
        user.setCreatedAt(UserDateTimeUtil.now());

        user.setImage(request.getImage());
        user.setDob(request.getDob());
        user.setNationality(request.getNationality());
        // Ensure code is 6-digit; if provided, sanitize; else derive from ID after save

        String code = agentIdGenerator.generate(request.getCurrency(), null);
        user.setCode(code);

        // Generate a random password
        String rawPassword = java.util.UUID.randomUUID().toString().substring(0, 8);
        user.setPassword(passwordEncoder.encode(rawPassword));

        User saved = userRepo.save(user);

        // Send welcome email with credentials
        try {
            String subject = "Welcome to " + platformName + " – Your Account is Ready!";
            String body = "Dear " + saved.getFullName() + ",\n\n"
                    + "Welcome to " + platformName + "! Your agency account has been successfully created.\n\n"
                    + "Here are your login credentials:\n"
                    + "Email: " + saved.getEmail() + "\n"
                    + "Password: " + rawPassword + "\n"
                    + "Agent Code: " + code + "\n\n"
                    + "Please change your password after your first login for security.\n\n"
                    + "If you have any questions, feel free to reach out to our support team.\n\n"
                    + "Best regards,\n"
                    + platformName + " Team";
            emailService.sendEmail(saved.getEmail(), subject, body);
        } catch (Exception e) {
            // Log but don't fail user creation if email fails
            System.err.println("Failed to send welcome email to " + saved.getEmail() + ": " + e.getMessage());
        }

        return saved.getId();
    }

    public void updateUser(Long id, UpdateUserRequest request) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        if (request.getEmail() != null) {
            String email = EmailUtils.normalize(request.getEmail());
            String currentEmail = user.getEmail() != null ? EmailUtils.normalize(user.getEmail()) : null;
            if (!email.equals(currentEmail) && userRepo.existsByEmail(email)) {
                throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "User with this email already exists");
            }
            user.setEmail(email);
        }
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getCurrency() != null) user.setCurrency(request.getCurrency());
        if (request.getBalance() != null) user.setBalance(request.getBalance());
        if (request.getPassportNumber() != null) user.setPassportNumber(request.getPassportNumber());

        if (request.getPassportExpiryDate() != null && !request.getPassportExpiryDate().isEmpty()) {
            user.setPassportExpiryDate(String.valueOf(LocalDate.parse(request.getPassportExpiryDate())));
        }


        if (request.getImage() != null) user.setImage(request.getImage());
        if (request.getDob() != null) user.setDob(request.getDob());
        if (request.getNationality() != null) user.setNationality(request.getNationality());


        if (request.getImage() != null) user.setImage(request.getImage());
        if (request.getTicketPreview() != null) user.setTicketPreview(request.getTicketPreview());
        if (request.getInvoicePreview() != null) user.setInvoicePreview(request.getInvoicePreview());
        if (request.getMoneyReceiptPreview() != null) user.setMoneyReceiptPreview(request.getMoneyReceiptPreview());


        userRepo.save(user);
    }


    public void deleteUser(Long id) {
        User user = userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User"));
        user.setDeleted(true);
        userRepo.save(user);
    }

    /**
     * Soft-delete an agency (mother) account. Blocked when any wallet transaction exists for the
     * agency user or any of its child users.
     */
    public void deleteAgency(Long id) {
        User user = userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User"));
        if (!user.isAgency()) {
            throw ServiceExceptions.insufficientBalance("User is not an agency account");
        }
        List<Long> scopeIds = new ArrayList<>();
        scopeIds.add(user.getId());
        for (User child : userRepo.findByParentUser_Id(user.getId())) {
            scopeIds.add(child.getId());
        }
        if (transactionRepository.countByUserIdIn(scopeIds) > 0) {
            throw ServiceExceptions.insufficientBalance("Cannot delete agency: wallet transactions exist for this agency or its sub-users");
        }
        deleteUser(id);
    }


    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepo.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw ServiceExceptions.insufficientBalance("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    public void resetPasswordByAdmin(Long userId, String newPassword) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    public Double getUserBalance(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User"));
        User walletUser = CreditLimitValidatorService.resolveWalletUser(user);
        return walletUser.getBalance() == null ? 0.0 : walletUser.getBalance();
    }

    @Transactional
    public void deductUserBalance(Long userId, Double amount, String providerName) {
        deductUserBalance(userId, amount, providerName, false, null, null, null, null);
    }

    @Transactional
    public void deductUserBalance(Long userId, Double amount, String providerName, boolean bypassBalanceCheck) {
        deductUserBalance(userId, amount, providerName, bypassBalanceCheck, null, null, null, null);
    }

    @Transactional
    public void deductUserBalance(Long userId, Double amount, String providerName, boolean bypassBalanceCheck,
                                  String source, Long referenceId, String referenceType, Long performedBy) {

        User user = userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User"));
        User walletUser = CreditLimitValidatorService.resolveWalletUser(user);

        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Amount must be greater than zero");
        }

        Double currentBalance = walletUser.getBalance() == null ? 0.0 : walletUser.getBalance();

        if (!bypassBalanceCheck) {
            double creditLimitVal = 0.0;
            try {
                java.util.Optional<BusinessEntity> businessOpt = businessRepository.findFirstByMotherUser(walletUser);
                if (businessOpt.isPresent() && businessOpt.get().getCreditLimit() != null) {
                    creditLimitVal = businessOpt.get().getCreditLimit().doubleValue();
                }
            } catch (Exception e) {
                // If we can't determine credit limit, just use 0
            }

            double creditUsed = CreditLimitValidatorService.calculateCreditUsed(currentBalance, amount);
            if (creditUsed > creditLimitVal) {
                double available = CreditLimitValidatorService.calculateAvailableBalance(currentBalance, creditLimitVal);
                throw ServiceExceptions.insufficientBalance("Insufficient balance. Available: " + available +
                        (creditLimitVal > 0 ? " (credit limit: " + creditLimitVal + ")" : ""));
            }
        }

        double balanceAfter = currentBalance - amount;
        int updated = userRepo.updateBalance(walletUser.getId(), balanceAfter);
        if (updated == 0) {
            throw new ResourceNotFoundException("User");
        }

        creditLimitService.applyCreditUsageForDebit(userId, currentBalance, amount, providerName, performedBy);

        balanceChangeHistoryRepository.save(BalanceChangeHistory.builder()
                .userId(walletUser.getId())
                .changeType("DEBIT")
                .amount(amount)
                .balanceBefore(currentBalance)
                .balanceAfter(balanceAfter)
                .reason(providerName)
                .source(source)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .performedBy(performedBy)
                .build());
    }

    @Transactional
    public void addUserBalance(Long userId, Double amount) {
        addUserBalance(userId, amount, null, null, null, null, null);
    }

    @Transactional
    public void addUserBalance(Long userId, Double amount,
                               String reason, String source, Long referenceId, String referenceType, Long performedBy) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Amount must be greater than zero");
        }

        User walletUser = CreditLimitValidatorService.resolveWalletUser(user);
        Double currentBalance = walletUser.getBalance() == null ? 0.0 : walletUser.getBalance();
        double balanceAfter   = currentBalance + amount;

        int updated = userRepo.updateBalance(walletUser.getId(), balanceAfter);
        if (updated == 0) {
            throw new ResourceNotFoundException("User");
        }

        balanceChangeHistoryRepository.save(BalanceChangeHistory.builder()
                .userId(walletUser.getId())
                .changeType("CREDIT")
                .amount(amount)
                .balanceBefore(currentBalance)
                .balanceAfter(balanceAfter)
                .reason(reason)
                .source(source)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .performedBy(performedBy)
                .build());
    }

    @Override
    @Transactional
    public void silentlyUndoBookingWalletDebits(Long actorUserId, Long bookingId, List<Double> purchaseAmounts) {
        if (purchaseAmounts == null || purchaseAmounts.isEmpty()) {
            return;
        }

        User walletUser = CreditLimitValidatorService.resolveWalletUser(
                userRepo.findById(actorUserId).orElseThrow(() -> new ResourceNotFoundException("User")));
        Long walletUserId = walletUser.getId();

        double restoreTotal = purchaseAmounts.stream().mapToDouble(Double::doubleValue).sum();
        if (restoreTotal <= 0) {
            return;
        }

        double currentBalance = walletUser.getBalance() != null ? walletUser.getBalance() : 0.0;
        userRepo.updateBalance(walletUserId, currentBalance + restoreTotal);

        List<Long> historyIds = new ArrayList<>();
        balanceChangeHistoryRepository
                .findByUserIdAndReferenceTypeAndReferenceIdAndChangeType(walletUserId, "BOOKING", bookingId, "DEBIT")
                .forEach(row -> historyIds.add(row.getId()));

        for (Double amount : purchaseAmounts) {
            balanceChangeHistoryRepository
                    .findFirstByUserIdAndReferenceTypeAndReferenceIdIsNullAndChangeTypeAndAmount(
                            walletUserId, "BOOKING", "DEBIT", amount)
                    .ifPresent(row -> {
                        if (!historyIds.contains(row.getId())) {
                            historyIds.add(row.getId());
                        }
                    });
        }

        if (!historyIds.isEmpty()) {
            balanceChangeHistoryRepository.deleteAllById(historyIds);
        }
    }


    public UserDto assignChildUser(ChildUserRequest request) {
        User parentUser = userRepo.findById(request.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent user"));

        Optional<BusinessEntity> business = businessRepository.findFirstByMotherUser(parentUser);

        if (business.isEmpty()) {
            throw ServiceExceptions.business("Parent user has no business assigned");
        }

        String email = EmailUtils.normalize(request.getEmail());
        if (userRepo.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "User with this email already exists");
        }

        String code = agentIdGenerator.generate(parentUser.getCurrency(), null);
        User childUser = new User();
        childUser.setEmail(email);
        childUser.setPassword(passwordEncoder.encode(request.getPassword()));
        childUser.setFullName(request.getFullName());
        childUser.setParentUser(parentUser);
        childUser.setBusiness(business.get());
        childUser.setVerified(true);
        childUser.setIsActive(true);
        childUser.setAgency(true);
        childUser.setCurrency(parentUser.getCurrency());
        childUser.setVoucherPreview(parentUser.getVoucherPreview());
        childUser.setMoneyReceiptPreview(parentUser.getMoneyReceiptPreview());
        childUser.setTicketPreview(parentUser.getTicketPreview());
        childUser.setCode(code);

        User savedUser = userRepo.save(childUser);
        return mapToResponse(savedUser);
    }


    public List<UserDto> getUsersByBusiness(Long businessId) {
        businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business"));

        List<User> users = userRepo.findByBusinessId(businessId);
        if (users == null) users = Collections.emptyList();

        return users.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public boolean isParentAccount(Long id) {
        User user = userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User"));
        return businessRepository.existsByMotherUser(user);
    }
    public void verifyUser(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User"));
        user.setVerified(true);
        userRepo.save(user);
    }
}
