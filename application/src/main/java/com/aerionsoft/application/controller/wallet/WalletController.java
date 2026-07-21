package com.aerionsoft.application.controller.wallet;


import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aerionsoft.application.annotation.SkipAutoAudit;
import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.dto.admin.bank.AdminChargeRequest;
import com.aerionsoft.application.dto.admin.bank.AdminDepositRequest;
import com.aerionsoft.application.dto.admin.bank.CheckoutSessionResponse;
import com.aerionsoft.application.dto.admin.bank.DepositApprovalRequest;
import com.aerionsoft.application.dto.admin.bank.DepositRequest;
import com.aerionsoft.application.dto.admin.bank.WalletDepositResponse;
import com.aerionsoft.application.dto.admin.bank.WalletStatementResponse;
import com.aerionsoft.application.dto.payment.PaymentRequestDto;
import com.aerionsoft.application.dto.payment.SslCommerzInitResponse;
import com.aerionsoft.application.dto.wallet.ServiceBalanceDeductionRequest;
import com.aerionsoft.application.dto.wallet.ServiceBalanceDeductionResponse;
import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.enums.wallet.DepositType;
import com.aerionsoft.application.service.common.FileStorageService;
import com.aerionsoft.application.service.payment.SslCommerzService;
import com.aerionsoft.application.service.user.UserService;
import com.aerionsoft.application.service.wallet.ServiceBalanceDeductionService;
import com.aerionsoft.application.service.wallet.WalletService;
import com.aerionsoft.application.service.admin.AdminUserService;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/wallet")
public class WalletController extends BaseController {

    @Autowired
    private WalletService walletService;
    @Autowired
    private SslCommerzService sslCommerzService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    UserService userService;
    @Autowired
    private AdminUserService adminUserService;
    @Autowired
    private ServiceBalanceDeductionService serviceBalanceDeductionService;

    private static final Logger log = LoggerFactory.getLogger(WalletController.class);

    @PostMapping(value = "/deposit")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-wallet-deposit')") // admin or user
    public ResponseEntity<BaseResponse<?>> deposit(
            Authentication authentication,
            @Valid @RequestPart("data") DepositRequest request,
            @RequestPart(value = "attachment", required = false) String attachment
    ) throws Exception {

        Long currentUserId = isAdmin()
                ? adminUserService.getUserByEmail(authentication.getName()).getId()
                : userService.getUserIdByEmail(authentication.getName());

        // Determine if this is a child user acting for parent
        Long targetUserId = currentUserId;
        Long actingUserId = null;

        if (!isAdmin()) {
            // Check if current user has a parent
            var currentUser = userService.getUserById(currentUserId);
            if (currentUser != null && currentUser.getParentUserId() != null) {
                // Child user - deposit goes to parent's account
                targetUserId = currentUser.getParentUserId();
                actingUserId = currentUserId;
            }
        }

        WalletDepositResponse deposit = walletService.createDeposit(targetUserId, request, attachment, actingUserId);
        return ResponseEntity.ok(BaseResponse.ok("Deposit created", deposit));
    }

    @GetMapping("/instant-deposit")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-wallet-instant-deposit')") // admin or user
    public ResponseEntity<BaseResponse<?>> instantDeposit(
            Authentication authentication,
            @RequestParam("amount") Double amount,
            @RequestParam(value = "remarks", required = false) String remarks
    ) {
        Long currentUserId = isAdmin()
                ? adminUserService.getUserByEmail(authentication.getName()).getId()
                : userService.getUserIdByEmail(authentication.getName());

        // Determine if this is a child user acting for parent
        Long targetUserId = currentUserId;

        if (!isAdmin()) {
            // Check if current user has a parent
            var currentUser = userService.getUserById(currentUserId);
            if (currentUser != null && currentUser.getParentUserId() != null) {
                // Child user - deposit goes to parent's account
                targetUserId = currentUser.getParentUserId();
            }
        }

        walletService.instantDeposit(targetUserId, amount, remarks);
        return ResponseEntity.ok(BaseResponse.ok("Deposit created Successfully"));
    }

    @PostMapping("/instant-deposit/checkout")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-wallet-instant-deposit')") // admin or user
    public ResponseEntity<BaseResponse<CheckoutSessionResponse>> createInstantDepositCheckout(
            Authentication authentication,
            @RequestParam("amount") Double amount,
            @RequestParam(value = "remarks", required = false) String remarks
    ) {
        Long currentUserId = isAdmin()
                ? adminUserService.getUserByEmail(authentication.getName()).getId()
                : userService.getUserIdByEmail(authentication.getName());

        // Determine if this is a child user acting for parent
        Long targetUserId = currentUserId;

        if (!isAdmin()) {
            // Check if current user has a parent
            var currentUser = userService.getUserById(currentUserId);
            if (currentUser != null && currentUser.getParentUserId() != null) {
                // Child user - deposit goes to parent's account
                targetUserId = currentUser.getParentUserId();
            }
        }

        CheckoutSessionResponse checkoutSession = walletService.createInstantDepositCheckoutSession(targetUserId, amount, remarks);
        return ResponseEntity.ok(BaseResponse.ok("Checkout session created successfully", checkoutSession));
    }

    // SSLCommerz wallet deposit endpoint (similar to Stripe's instant deposit)
    @PostMapping("/ssl/deposit")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-wallet-ssl-deposit')") // admin or user
    public ResponseEntity<BaseResponse<SslCommerzInitResponse>> sslWalletDeposit(
            Authentication authentication,
            @RequestParam("amount") Double amount,
            @RequestParam(value = "remarks", required = false) String remarks) {

        Long currentUserId = isAdmin()
                ? adminUserService.getUserByEmail(authentication.getName()).getId()
                : userService.getUserIdByEmail(authentication.getName());

        // Determine if this is a child user acting for parent
        Long targetUserId = currentUserId;

        if (!isAdmin()) {
            // Check if current user has a parent
            var currentUser = userService.getUserById(currentUserId);
            if (currentUser != null && currentUser.getParentUserId() != null) {
                // Child user - deposit goes to parent's account
                targetUserId = currentUser.getParentUserId();
            }
        }

        SslCommerzInitResponse response = sslCommerzService.createWalletDepositSession(targetUserId, amount, remarks);
        return ResponseEntity.ok(BaseResponse.ok("SSLCommerz wallet deposit initiated", response));
    }


    // List all my deposits
    @GetMapping("/deposits")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-wallet-deposit')") // admin or user
    public ResponseEntity<BaseResponse<Page<WalletDepositResponse>>> myDeposits(
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) DepositStatus status,
            @RequestParam(required = false) DepositType type,
            @RequestParam(required = false) Boolean isDebit,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean includeTotal,
            Authentication authentication) {

        Long userId = null;

        String provider = getProviderName(authentication);

        boolean admin = "admin".equalsIgnoreCase(provider);

        if (!admin) {
            userId = getUserIdFromAuthentication();
        } else{
            userId = getUserIdFromAuthentication();
        }
        return ResponseEntity.ok(BaseResponse.ok("My deposits", walletService.getUserDeposits(currency, userId, page, size, admin, status, type, provider, isDebit, from, to, includeTotal)));
    }

    // List all pending approvals (admin)
    @GetMapping("/approvals")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-wallet-pending-deposit')")
    public ResponseEntity<BaseResponse<Page<WalletDepositResponse>>> approvalList(
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<WalletDepositResponse> pending = walletService.getPendingDeposits(currency, page, size, from, to);
        return ResponseEntity.ok(BaseResponse.ok("Pending deposits", pending));
    }

    // Approve/Reject a deposit (admin)
    @PostMapping("/approvals/{depositId}")
    @SkipAutoAudit
    @PreAuthorize("@permissionService.hasPermission(authentication, 'approve-reject-wallet-deposit')")
    public ResponseEntity<BaseResponse<WalletDepositResponse>> approveOrReject(
            @PathVariable Long depositId,
            @Valid @RequestBody DepositApprovalRequest req,
            Authentication authentication) {
        Long adminId = getUserIdFromAuthentication();

        WalletDepositResponse resp = walletService.approveOrReject(depositId, req.getStatus(), adminId, req.getAdminRemarks());
        return ResponseEntity.ok(BaseResponse.ok("Deposit " + req.getStatus().name().toLowerCase(), resp));
    }

//    @PostMapping("/withdraw")
//    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-wallet-withdraw')") // admin or user
//    public ResponseEntity<BaseResponse<WalletDepositResponse>> withdraw(
//            Authentication authentication,
//            @Valid @RequestBody DepositRequest req
////            ,@RequestParam(value = "attachment", required = false) MultipartFile attachment
//    ) {
////        String filename = null;
////        try {
////            if (attachment != null) {
////                filename = fileStorageService.saveFile(attachment, "withdraw");
////            }
////        } catch (IOException e) {
////            throw new RuntimeException("Failed to save attachment");
////        }
//
//        Long currentUserId = isAdmin()
//                ? adminUserService.getUserByEmail(authentication.getName()).getId()
//                : userService.getUserIdByEmail(authentication.getName());
//
//        // Determine if this is a child user acting for parent
//        Long targetUserId = currentUserId;
//        Long actingUserId = null;
//
//        if (!isAdmin()) {
//            // Check if current user has a parent
//            var currentUser = userService.getUserById(currentUserId);
//            if (currentUser != null && currentUser.getParentUserId() != null) {
//                // Child user - deposit goes to parent's account
//                targetUserId = currentUser.getParentUserId();
//                actingUserId = currentUserId;
//            }
//        }
//        WalletDepositResponse withdrawal = walletService.createWithdrawal(targetUserId, req, null, actingUserId);
//        return ResponseEntity.ok(BaseResponse.ok("Withdrawal requested", withdrawal));
//    }

    @PostMapping("/refund")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-wallet-refund')") // admin or user
    public ResponseEntity<BaseResponse<WalletDepositResponse>> refund(
            Authentication authentication,
            @RequestParam("amount") Double amount,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "attachment", required = false) MultipartFile attachment
    ) {
        String filename = null;
        if (attachment != null) {
            try {
                filename = fileStorageService.saveFile(attachment, "refund");
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.INVALID_FILE, "Failed to save refund attachment");
            }
        }

        Long userId = getUserIdFromAuthentication();
        WalletDepositResponse refund = walletService.createRefund(userId, amount, remarks, filename);
        return ResponseEntity.ok(BaseResponse.ok("Refund requested", refund));
    }

    @GetMapping("/statement")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-wallet-statement')") // admin or user
    public ResponseEntity<BaseResponse<List<WalletStatementResponse>>> statement(
            Authentication authentication,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Long userId = getUserIdFromAuthentication();
        List<WalletStatementResponse> list = walletService.getStatement(userId, from, to);
        return ResponseEntity.ok(BaseResponse.ok("Statement", list));
    }

    @PostMapping("/ngenius/deposit")
    public ResponseEntity<?> nGeniusDeposit(@Valid @RequestBody PaymentRequestDto dto, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        Long currentUserId = isAdmin()
                ? adminUserService.getUserByEmail(authentication.getName()).getId()
                : userService.getUserIdByEmail(authentication.getName());

        // Determine if this is a child user acting for parent
        Long targetUserId = currentUserId;
        Long actingUserId = null;

        if (!isAdmin()) {
            // Check if current user has a parent
            var currentUser = userService.getUserById(currentUserId);
            if (currentUser != null && currentUser.getParentUserId() != null) {
                // Child user - deposit goes to parent's account
                targetUserId = currentUser.getParentUserId();
            }
        }
        //issue need to fix
//        Object result = walletService.createNgeniusWalletDeposit(dto, provider, authUserId, targetUserId);

        return ResponseEntity.ok(null);
    }

    /**
     * Deduct wallet balance for visa, tour, or hotel services.
     * Validates available balance including credit limit before debiting.
     *
     * POST /api/wallet/service-deduct
     */
    @PostMapping("/service-deduct")
    @SkipAutoAudit
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ServiceBalanceDeductionResponse>> serviceDeduct(
            Authentication authentication,
            @Valid @RequestBody ServiceBalanceDeductionRequest request) {
        Long currentUserId = getUserIdFromAuthentication();
        Long targetUserId = currentUserId;
        Long actingUserId = null;

        if (isAdmin() && request.getUserId() != null) {
            targetUserId = request.getUserId();
        } else if (!isAdmin()) {
            var currentUser = userService.getUserById(currentUserId);
            if (currentUser != null && currentUser.getParentUserId() != null) {
                targetUserId = currentUser.getParentUserId();
                actingUserId = currentUserId;
            }
        }

        String providerName = getProviderName(authentication);
        ServiceBalanceDeductionResponse response = serviceBalanceDeductionService.deduct(
                request, targetUserId, actingUserId, providerName);

        return ResponseEntity.ok(BaseResponse.ok(
                response,
                request.getServiceType().name().toLowerCase() + " balance deducted successfully"));
    }

    /**
     * Admin: immediately deduct (charge) an amount from a user's wallet.
     *
     * - Creates a WalletDeposit record with type=ADMIN_CHARGE, status=APPROVED.
     * - Deducts the amount from the user balance right away (no approval flow needed).
     * - Records an active Transaction entry in the ledger/history.
     *
     * POST /api/wallet/admin/charge
     * Permission: admin-wallet-charge
     */
    @PostMapping("/admin/charge")
    @SkipAutoAudit
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-wallet-charge')")
    public ResponseEntity<BaseResponse<WalletDepositResponse>> adminCharge(
            @Valid @RequestBody AdminChargeRequest req) {
        Long adminId = getUserIdFromAuthentication();
        WalletDepositResponse response = walletService.adminCharge(req, adminId);
        return ResponseEntity.ok(BaseResponse.ok(
                "Wallet charge of " + req.getAmount() + " " + req.getCurrency() +
                " applied to user " + req.getUserId() + " successfully", response));
    }

    /**
     * Admin directly deposits an amount into a user's wallet.
     * Requires motherUserId, imageUrl (proof of payment), amount, and currency.
     * The deposit is immediately approved and the user's balance is credited.
     *
     * POST /api/wallet/admin/deposit
     * Permission: admin-wallet-deposit
     */
    @PostMapping("/admin/deposit")
    @SkipAutoAudit
    public ResponseEntity<BaseResponse<WalletDepositResponse>> adminDeposit(
            @Valid @RequestBody AdminDepositRequest req) {
        Long adminId = getUserIdFromAuthentication();
        WalletDepositResponse response = walletService.adminDeposit(req, adminId);
        return ResponseEntity.ok(BaseResponse.ok(
                "Deposit of " + req.getAmount() + " " + req.getCurrency() +
                " credited to user " + req.getMotherUserId() + " successfully", response));
    }

}
