$ErrorActionPreference = "Stop"
$root = "d:\java\flight-backend"
Set-Location $root

$dirs = @(
    "src/main/java/com/example/tufantrip/controller/common",
    "src/main/java/com/example/tufantrip/controller/payment",
    "src/main/java/com/example/tufantrip/controller/expense",
    "src/main/java/com/example/tufantrip/controller/finance",
    "src/main/java/com/example/tufantrip/controller/notification",
    "src/main/java/com/example/tufantrip/service/booking",
    "src/main/java/com/example/tufantrip/service/tour",
    "src/main/java/com/example/tufantrip/service/visa",
    "src/main/java/com/example/tufantrip/service/expense",
    "src/main/java/com/example/tufantrip/service/finance",
    "src/main/java/com/example/tufantrip/service/user",
    "src/main/java/com/example/tufantrip/service/business",
    "src/main/java/com/example/tufantrip/service/common"
)
foreach ($d in $dirs) { New-Item -ItemType Directory -Force -Path $d | Out-Null }

function Move-File($from, $to) {
    if (Test-Path $from) {
        git mv $from $to
    }
}

# Controllers
Move-File "src/main/java/com/example/tufantrip/controller/TravellerController.java" "src/main/java/com/example/tufantrip/controller/booking/TravellerController.java"
Move-File "src/main/java/com/example/tufantrip/controller/TicketActionRequestAdminController.java" "src/main/java/com/example/tufantrip/controller/booking/TicketActionRequestAdminController.java"
Move-File "src/main/java/com/example/tufantrip/controller/MarkupController.java" "src/main/java/com/example/tufantrip/controller/flight/MarkupController.java"
Move-File "src/main/java/com/example/tufantrip/controller/PaymentController.java" "src/main/java/com/example/tufantrip/controller/payment/PaymentController.java"
Move-File "src/main/java/com/example/tufantrip/controller/StripeWebhookController.java" "src/main/java/com/example/tufantrip/controller/payment/StripeWebhookController.java"
Move-File "src/main/java/com/example/tufantrip/controller/ExpenseController.java" "src/main/java/com/example/tufantrip/controller/expense/ExpenseController.java"
Move-File "src/main/java/com/example/tufantrip/controller/ExpenseDetailController.java" "src/main/java/com/example/tufantrip/controller/expense/ExpenseDetailController.java"
Move-File "src/main/java/com/example/tufantrip/controller/AccountHeadController.java" "src/main/java/com/example/tufantrip/controller/finance/AccountHeadController.java"
Move-File "src/main/java/com/example/tufantrip/controller/NotificationController.java" "src/main/java/com/example/tufantrip/controller/notification/NotificationController.java"
Move-File "src/main/java/com/example/tufantrip/controller/HealthController.java" "src/main/java/com/example/tufantrip/controller/common/HealthController.java"
Move-File "src/main/java/com/example/tufantrip/controller/CountriesController.java" "src/main/java/com/example/tufantrip/controller/common/CountriesController.java"
Move-File "src/main/java/com/example/tufantrip/controller/CurrencyController.java" "src/main/java/com/example/tufantrip/controller/common/CurrencyController.java"
Move-File "src/main/java/com/example/tufantrip/controller/FileUploadController.java" "src/main/java/com/example/tufantrip/controller/common/FileUploadController.java"
Move-File "src/main/java/com/example/tufantrip/controller/PublicRouteController.java" "src/main/java/com/example/tufantrip/controller/common/PublicRouteController.java"

# Services - wallet
Move-File "src/main/java/com/example/tufantrip/service/WalletService.java" "src/main/java/com/example/tufantrip/service/wallet/WalletService.java"
Move-File "src/main/java/com/example/tufantrip/service/LedgerService.java" "src/main/java/com/example/tufantrip/service/wallet/LedgerService.java"
Move-File "src/main/java/com/example/tufantrip/service/ReferenceGeneratorService.java" "src/main/java/com/example/tufantrip/service/wallet/ReferenceGeneratorService.java"
Move-File "src/main/java/com/example/tufantrip/service/DepositBankService.java" "src/main/java/com/example/tufantrip/service/wallet/DepositBankService.java"
Move-File "src/main/java/com/example/tufantrip/service/CreditLimitService.java" "src/main/java/com/example/tufantrip/service/wallet/CreditLimitService.java"
Move-File "src/main/java/com/example/tufantrip/service/CreditLimitServiceImpl.java" "src/main/java/com/example/tufantrip/service/wallet/CreditLimitServiceImpl.java"
Move-File "src/main/java/com/example/tufantrip/service/CreditLimitValidatorService.java" "src/main/java/com/example/tufantrip/service/wallet/CreditLimitValidatorService.java"
Move-File "src/main/java/com/example/tufantrip/service/transaction/TransactionService.java" "src/main/java/com/example/tufantrip/service/wallet/TransactionService.java"

# Services - booking
Move-File "src/main/java/com/example/tufantrip/service/BookingService.java" "src/main/java/com/example/tufantrip/service/booking/BookingService.java"
Move-File "src/main/java/com/example/tufantrip/service/BookingCoordinatorService.java" "src/main/java/com/example/tufantrip/service/booking/BookingCoordinatorService.java"
Move-File "src/main/java/com/example/tufantrip/service/BookingTimelineService.java" "src/main/java/com/example/tufantrip/service/booking/BookingTimelineService.java"
Move-File "src/main/java/com/example/tufantrip/service/BookingPriceService.java" "src/main/java/com/example/tufantrip/service/booking/BookingPriceService.java"
Move-File "src/main/java/com/example/tufantrip/service/BookingSupplierInvoiceService.java" "src/main/java/com/example/tufantrip/service/booking/BookingSupplierInvoiceService.java"
Move-File "src/main/java/com/example/tufantrip/service/TicketingDeadlineService.java" "src/main/java/com/example/tufantrip/service/booking/TicketingDeadlineService.java"
Move-File "src/main/java/com/example/tufantrip/service/TravellerService.java" "src/main/java/com/example/tufantrip/service/booking/TravellerService.java"
Move-File "src/main/java/com/example/tufantrip/service/TicketActionRequestService.java" "src/main/java/com/example/tufantrip/service/booking/TicketActionRequestService.java"

# Services - tour / visa
Move-File "src/main/java/com/example/tufantrip/service/TourApplicationService.java" "src/main/java/com/example/tufantrip/service/tour/TourApplicationService.java"
Move-File "src/main/java/com/example/tufantrip/service/client/VisaApplicationService.java" "src/main/java/com/example/tufantrip/service/visa/VisaApplicationService.java"
Move-File "src/main/java/com/example/tufantrip/service/client/VisaApplicationServiceImpl.java" "src/main/java/com/example/tufantrip/service/visa/VisaApplicationServiceImpl.java"
Move-File "src/main/java/com/example/tufantrip/service/admin/TourPackageService.java" "src/main/java/com/example/tufantrip/service/tour/TourPackageService.java"
Move-File "src/main/java/com/example/tufantrip/service/admin/TourPackageServiceImpl.java" "src/main/java/com/example/tufantrip/service/tour/TourPackageServiceImpl.java"

# Services - expense / finance / payment
Move-File "src/main/java/com/example/tufantrip/service/ExpenseService.java" "src/main/java/com/example/tufantrip/service/expense/ExpenseService.java"
Move-File "src/main/java/com/example/tufantrip/service/ExpenseDetailService.java" "src/main/java/com/example/tufantrip/service/expense/ExpenseDetailService.java"
Move-File "src/main/java/com/example/tufantrip/service/AccountHeadService.java" "src/main/java/com/example/tufantrip/service/finance/AccountHeadService.java"
Move-File "src/main/java/com/example/tufantrip/service/SslCommerzService.java" "src/main/java/com/example/tufantrip/service/payment/SslCommerzService.java"

# Services - user / business / common
Move-File "src/main/java/com/example/tufantrip/service/UserService.java" "src/main/java/com/example/tufantrip/service/user/UserService.java"
Move-File "src/main/java/com/example/tufantrip/service/UserCoordinatorService.java" "src/main/java/com/example/tufantrip/service/user/UserCoordinatorService.java"
Move-File "src/main/java/com/example/tufantrip/service/UserCleanupService.java" "src/main/java/com/example/tufantrip/service/user/UserCleanupService.java"
Move-File "src/main/java/com/example/tufantrip/service/UserDetailsServiceImpl.java" "src/main/java/com/example/tufantrip/service/user/UserDetailsServiceImpl.java"
Move-File "src/main/java/com/example/tufantrip/service/CustomUserDetails.java" "src/main/java/com/example/tufantrip/service/user/CustomUserDetails.java"
Move-File "src/main/java/com/example/tufantrip/service/BusinessService.java" "src/main/java/com/example/tufantrip/service/business/BusinessService.java"
Move-File "src/main/java/com/example/tufantrip/service/BusinessServiceImpl.java" "src/main/java/com/example/tufantrip/service/business/BusinessServiceImpl.java"
Move-File "src/main/java/com/example/tufantrip/service/BusinessProviderService.java" "src/main/java/com/example/tufantrip/service/business/BusinessProviderService.java"
Move-File "src/main/java/com/example/tufantrip/service/BusinessProviderServiceImpl.java" "src/main/java/com/example/tufantrip/service/business/BusinessProviderServiceImpl.java"
Move-File "src/main/java/com/example/tufantrip/service/BusinessSalesPersonService.java" "src/main/java/com/example/tufantrip/service/business/BusinessSalesPersonService.java"
Move-File "src/main/java/com/example/tufantrip/service/BusinessSalesPersonServiceImpl.java" "src/main/java/com/example/tufantrip/service/business/BusinessSalesPersonServiceImpl.java"
Move-File "src/main/java/com/example/tufantrip/service/SalesPersonService.java" "src/main/java/com/example/tufantrip/service/business/SalesPersonService.java"
Move-File "src/main/java/com/example/tufantrip/service/CountriesService.java" "src/main/java/com/example/tufantrip/service/common/CountriesService.java"
Move-File "src/main/java/com/example/tufantrip/service/CurrencyService.java" "src/main/java/com/example/tufantrip/service/common/CurrencyService.java"
Move-File "src/main/java/com/example/tufantrip/service/FileStorageService.java" "src/main/java/com/example/tufantrip/service/common/FileStorageService.java"
Move-File "src/main/java/com/example/tufantrip/service/GlobalSearchService.java" "src/main/java/com/example/tufantrip/service/common/GlobalSearchService.java"

Write-Host "File moves complete."
