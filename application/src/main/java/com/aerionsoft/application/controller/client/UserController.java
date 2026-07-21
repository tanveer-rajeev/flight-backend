package com.aerionsoft.application.controller.client;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.business.BusinessDto;
import com.aerionsoft.application.dto.ChildUserRequest;
import com.aerionsoft.application.dto.client.user.*;
import com.aerionsoft.application.dto.rolepermission.response.RoleResponseDto;
import com.aerionsoft.application.dto.wallet.BalanceChangeHistoryDTO;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.wallet.BalanceChangeHistory;
import com.aerionsoft.application.repository.wallet.BalanceChangeHistoryRepository;
import com.aerionsoft.application.repository.booking.BookingRepository;
import com.aerionsoft.application.util.TimestampMapper;
import com.aerionsoft.application.service.business.BusinessService;
import com.aerionsoft.application.service.access.RoleService;
import com.aerionsoft.application.service.user.UserService;
import com.aerionsoft.application.service.client.ClientDashBoardService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/user")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;
    @Autowired
    private ClientDashBoardService clientDashBoardService;
    @Autowired
    private RoleService roleService;
    @Autowired
    BusinessService businessService;
    @Autowired
    private BalanceChangeHistoryRepository balanceChangeHistoryRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private TimestampMapper timestampMapper;

    @PostMapping("/assign-child")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'assign-child-user')")
    public ResponseEntity<BaseResponse<UserDto>> assignChildUser(@Valid @RequestBody ChildUserRequest request) {

        Long currentUserId = getUserIdFromAuthentication();

        // Check if current user is a mother/parent account
        if (!userService.isParentAccount(currentUserId)) {
            return ResponseEntity.badRequest()
                    .body(BaseResponse.error("Only parent accounts can assign child users"));
        }

        request.setParentId(currentUserId);
        UserDto childUser = userService.assignChildUser(request);
        return ResponseEntity.ok(BaseResponse.ok(childUser));
    }

    @GetMapping("/business/{businessId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-business-user')")
    public ResponseEntity<BaseResponse<List<UserDto>>> getUsersByBusiness(@PathVariable Long businessId) {
        List<UserDto> users = userService.getUsersByBusiness(businessId);
        return ResponseEntity.ok(BaseResponse.ok(users));
    }


    @GetMapping("/business")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-business-user')")
    public ResponseEntity<BaseResponse<BusinessDto>> getOwnBusiness(Authentication authentication) {
        Long userId = getUserIdFromAuthentication();
        BusinessDto business = businessService.getBusinessByUserId(userId);
        return ResponseEntity.ok(BaseResponse.ok(business));
    }



    @GetMapping("/profile")
    public ResponseEntity<BaseResponse<UserDto>> getProfile(Authentication authentication) {
        String email = authentication.getName();
        UserDto user = userService.getProfile(email);
        return ResponseEntity.ok(BaseResponse.ok(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<BaseResponse<User>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest req) {

        String email = authentication.getName();
        User user = userService.updateProfile(email, req);
        return ResponseEntity.ok(BaseResponse.ok(user));
    }

    @PostMapping("/profile/image")
    public ResponseEntity<BaseResponse<String>> uploadProfileImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) throws IOException {

        String email = authentication.getName();
        String filename = userService.uploadProfileImage(email, file);
        String fileUri = "/uploads/" + filename;
        return ResponseEntity.ok(BaseResponse.ok(fileUri));
    }

    @PostMapping("/create")
    public ResponseEntity<BaseResponse<String>> createUser(@Valid @RequestBody CreateUserRequest request) {
        userService.createUser(request,true);
        return ResponseEntity.ok(BaseResponse.ok("User created successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<BaseResponse<String>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        userService.updateUser(id, request);
        return ResponseEntity.ok(BaseResponse.ok("User updated successfully"));
    }

    @PutMapping("/verify/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<BaseResponse<String>> verifyUser(@PathVariable Long id) {
        userService.verifyUser(id);
        return ResponseEntity.ok(BaseResponse.ok("User verified successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<BaseResponse<String>> deleteUser(@Valid @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(BaseResponse.ok("User deleted successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<UserDto>> getUserById(@PathVariable Long id) {
        Long userId = getUserIdFromAuthentication();
        UserDto user = userService.getUserById(userId);
        return ResponseEntity.ok(BaseResponse.ok(user));
    }

//    @GetMapping("/list")
//    @PreAuthorize("hasRole('ADadmin")
//    public ResponseEntity<BaseResponse<Page<UserDto>>> getUserList(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(required = false) String name,
//            @RequestParam(required = false) Boolean isAgency,
//            @RequestParam(required = false) Integer status,
//            @RequestParam(required = false) LocalDate creationDate) {
//
//        Page<UserDto> userList = userService.getUserList(page, size, name, isAgency, status, creationDate);
//        return ResponseEntity.ok(BaseResponse.ok(userList));
//    }

    @PostMapping("/password-change")
    public ResponseEntity<BaseResponse<String>> changePassword(
            Authentication authentication,
            @Valid @RequestBody PasswordChangeRequest request) {
        String email = authentication.getName();
        userService.changePassword(email, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(BaseResponse.ok("Password changed successfully"));
    }


    @GetMapping("/dashboard")
    public ResponseEntity<BaseResponse<DashboardStatsDto>> getDashboardData(Authentication authentication) {
        Long currentUserId = getUserIdFromAuthentication();

        // Determine if this is a child user - fetch parent's dashboard data
        Long targetUserId = currentUserId;
        var currentUser = userService.getUserById(currentUserId);
        if (currentUser != null && currentUser.getParentUserId() != null) {
            // Child user - fetch parent's dashboard data
            targetUserId = currentUser.getParentUserId();
        }

        DashboardStatsDto dashboardData = clientDashBoardService.getDashboardData(targetUserId);
        return ResponseEntity.ok(BaseResponse.ok(dashboardData));
    }

    @GetMapping("transactions")
    public ResponseEntity<BaseResponse<Page<TransactionDto>>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long currentUserId = getUserIdFromAuthentication();

        // Determine if this is a child user - fetch parent's transactions
        Long targetUserId = currentUserId;
        var currentUser = userService.getUserById(currentUserId);
        if (currentUser != null && currentUser.getParentUserId() != null) {
            // Child user - fetch parent's transactions
            targetUserId = currentUser.getParentUserId();
        }

        Page<TransactionDto> transactions = clientDashBoardService.getTransactions(targetUserId, page, size);
        return ResponseEntity.ok(BaseResponse.ok(transactions));
    }

    @GetMapping("transactions/user")
    public ResponseEntity<BaseResponse<Page<TransactionDto>>> getTransactionsUser(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Page<TransactionDto> transactions = clientDashBoardService.getTransactions(userId, page, size);
        return ResponseEntity.ok(BaseResponse.ok(transactions));
    }

    @GetMapping("{agencyId}/roles")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-role')")
    public ResponseEntity<BaseResponse<List<RoleResponseDto>>> getRolesByAgency(@PathVariable Long agencyId,  @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        List<RoleResponseDto> roleList = roleService.getRolesByAgencyId(agencyId, page, size);

        return ResponseEntity.ok(BaseResponse.ok(roleList));
    }

    /**
     * Balance change history for a given user.
     * If userId is not provided, falls back to the authenticated user.
     * Child users automatically see their parent's history (same wallet).
     *
     * GET /api/user/balance-history
     * Query params:
     *   userId     – target user ID (optional, defaults to authenticated user)
     *   changeType – "CREDIT" or "DEBIT" (optional)
     *   from       – start date (optional)
     *   to         – end date (optional)
     *   page       – default 0
     *   size       – default 20
     */
    @GetMapping("/balance-history")
    public ResponseEntity<BaseResponse<Page<BalanceChangeHistoryDTO>>> getBalanceHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String changeType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // If userId not supplied, use the authenticated user
        Long targetUserId = (userId != null) ? userId : getUserIdFromAuthentication();

        // Child users share their parent's wallet — resolve to parent
        var targetUser = userService.getUserById(targetUserId);
        if (targetUser != null && targetUser.getParentUserId() != null) {
            targetUserId = targetUser.getParentUserId();
        }

        String typeFilter = changeType != null ? changeType.trim().toUpperCase() : null;

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<BalanceChangeHistory> spec = BalanceChangeHistoryRepository.forUser(targetUserId)
                .and(BalanceChangeHistoryRepository.hasChangeType(typeFilter))
                .and(BalanceChangeHistoryRepository.createdInUserRange(from, to));

        Page<BalanceChangeHistoryDTO> result = balanceChangeHistoryRepository
                .findAll(spec, pageable)
                .map(this::toDTO);

        return ResponseEntity.ok(BaseResponse.ok("Balance history retrieved successfully", result));
    }

    private BalanceChangeHistoryDTO toDTO(BalanceChangeHistory h) {
        BalanceChangeHistoryDTO.TicketInfo ticketInfo = null;
        if ("BOOKING".equalsIgnoreCase(h.getReferenceType()) && h.getReferenceId() != null) {
            ticketInfo = bookingRepository.findById(h.getReferenceId())
                    .map(b -> BalanceChangeHistoryDTO.TicketInfo.builder()
                            .bookingId(b.getId())
                            .bookingReference(b.getBookingReference())
                            .pnr(b.getPnr())
                            .ticketNo(b.getTicketNo())
                            .airline(b.getAirline())
                            .airlinePnrs(b.getAirlinePnrs())
                            .status(b.getStatus())
                            .build())
                    .orElse(null);
        }
        return BalanceChangeHistoryDTO.builder()
                .id(h.getId())
                .userId(h.getUserId())
                .changeType(h.getChangeType())
                .amount(h.getAmount())
                .balanceBefore(h.getBalanceBefore())
                .balanceAfter(h.getBalanceAfter())
                .reason(h.getReason())
                .source(h.getSource())
                .referenceId(h.getReferenceId())
                .referenceType(h.getReferenceType())
                .performedBy(h.getPerformedBy())
                .createdAt(timestampMapper.toRequestUserTime(h.getCreatedAt(), h.getCreatedTimeOffset()))
                .ticket(ticketInfo)
                .build();
    }
}