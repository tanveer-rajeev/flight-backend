$ErrorActionPreference = "Stop"
$root = "d:\java\flight-backend"
Set-Location $root

# Update package declarations in moved controller/service files
$packageMap = @{
    "src/main/java/com/example/tufantrip/controller/booking"     = "com.example.tufantrip.controller.booking"
    "src/main/java/com/example/tufantrip/controller/wallet"      = "com.example.tufantrip.controller.wallet"
    "src/main/java/com/example/tufantrip/controller/tour"        = "com.example.tufantrip.controller.tour"
    "src/main/java/com/example/tufantrip/controller/visa"        = "com.example.tufantrip.controller.visa"
    "src/main/java/com/example/tufantrip/controller/flight"      = "com.example.tufantrip.controller.flight"
    "src/main/java/com/example/tufantrip/controller/payment"     = "com.example.tufantrip.controller.payment"
    "src/main/java/com/example/tufantrip/controller/expense"     = "com.example.tufantrip.controller.expense"
    "src/main/java/com/example/tufantrip/controller/finance"     = "com.example.tufantrip.controller.finance"
    "src/main/java/com/example/tufantrip/controller/notification"= "com.example.tufantrip.controller.notification"
    "src/main/java/com/example/tufantrip/controller/common"      = "com.example.tufantrip.controller.common"
    "src/main/java/com/example/tufantrip/service/wallet"         = "com.example.tufantrip.service.wallet"
    "src/main/java/com/example/tufantrip/service/booking"        = "com.example.tufantrip.service.booking"
    "src/main/java/com/example/tufantrip/service/tour"           = "com.example.tufantrip.service.tour"
    "src/main/java/com/example/tufantrip/service/visa"           = "com.example.tufantrip.service.visa"
    "src/main/java/com/example/tufantrip/service/expense"        = "com.example.tufantrip.service.expense"
    "src/main/java/com/example/tufantrip/service/finance"        = "com.example.tufantrip.service.finance"
    "src/main/java/com/example/tufantrip/service/user"           = "com.example.tufantrip.service.user"
    "src/main/java/com/example/tufantrip/service/business"       = "com.example.tufantrip.service.business"
    "src/main/java/com/example/tufantrip/service/common"         = "com.example.tufantrip.service.common"
}

foreach ($entry in $packageMap.GetEnumerator()) {
    $dir = $entry.Key
    $pkg = $entry.Value
    if (-not (Test-Path $dir)) { continue }
    Get-ChildItem $dir -Filter "*.java" | Where-Object { $_.Name -ne "package-info.java" } | ForEach-Object {
        $content = Get-Content $_.FullName -Raw
        $content = $content -replace '(?m)^package\s+com\.example\.tufantrip\.(controller|service)(\.\w+)*;', "package $pkg;"
        Set-Content $_.FullName $content -NoNewline
    }
}

# Bulk import replacements across all Java sources
$replacements = [ordered]@{
    "import com.example.tufantrip.service.WalletService;" = "import wallet.service.com.aerionsoft.application.WalletService;"
    "import com.example.tufantrip.service.LedgerService;" = "import wallet.service.com.aerionsoft.application.LedgerService;"
    "import com.example.tufantrip.service.ReferenceGeneratorService;" = "import wallet.service.com.aerionsoft.application.ReferenceGeneratorService;"
    "import com.example.tufantrip.service.DepositBankService;" = "import wallet.service.com.aerionsoft.application.DepositBankService;"
    "import com.example.tufantrip.service.CreditLimitService;" = "import wallet.service.com.aerionsoft.application.CreditLimitService;"
    "import com.example.tufantrip.service.CreditLimitServiceImpl;" = "import wallet.service.com.aerionsoft.application.CreditLimitServiceImpl;"
    "import com.example.tufantrip.service.CreditLimitValidatorService;" = "import wallet.service.com.aerionsoft.application.CreditLimitValidatorService;"
    "import com.example.tufantrip.service.transaction.TransactionService;" = "import wallet.service.com.aerionsoft.application.TransactionService;"
    "import com.example.tufantrip.service.BookingService;" = "import booking.service.com.aerionsoft.application.BookingService;"
    "import com.example.tufantrip.service.BookingCoordinatorService;" = "import booking.service.com.aerionsoft.application.BookingCoordinatorService;"
    "import com.example.tufantrip.service.BookingTimelineService;" = "import booking.service.com.aerionsoft.application.BookingTimelineService;"
    "import com.example.tufantrip.service.BookingPriceService;" = "import booking.service.com.aerionsoft.application.BookingPriceService;"
    "import com.example.tufantrip.service.BookingSupplierInvoiceService;" = "import booking.service.com.aerionsoft.application.BookingSupplierInvoiceService;"
    "import com.example.tufantrip.service.TicketingDeadlineService;" = "import booking.service.com.aerionsoft.application.TicketingDeadlineService;"
    "import com.example.tufantrip.service.TravellerService;" = "import booking.service.com.aerionsoft.application.TravellerService;"
    "import com.example.tufantrip.service.TicketActionRequestService;" = "import booking.service.com.aerionsoft.application.TicketActionRequestService;"
    "import com.example.tufantrip.service.TourApplicationService;" = "import tour.service.com.aerionsoft.application.TourApplicationService;"
    "import com.example.tufantrip.service.admin.TourPackageService;" = "import tour.service.com.aerionsoft.application.TourPackageService;"
    "import com.example.tufantrip.service.admin.TourPackageServiceImpl;" = "import tour.service.com.aerionsoft.application.TourPackageServiceImpl;"
    "import com.example.tufantrip.service.client.VisaApplicationService;" = "import visa.service.com.aerionsoft.application.VisaApplicationService;"
    "import com.example.tufantrip.service.client.VisaApplicationServiceImpl;" = "import visa.service.com.aerionsoft.application.VisaApplicationServiceImpl;"
    "import com.example.tufantrip.service.ExpenseService;" = "import expense.service.com.aerionsoft.application.ExpenseService;"
    "import com.example.tufantrip.service.ExpenseDetailService;" = "import expense.service.com.aerionsoft.application.ExpenseDetailService;"
    "import com.example.tufantrip.service.AccountHeadService;" = "import finance.service.com.aerionsoft.application.AccountHeadService;"
    "import com.example.tufantrip.service.SslCommerzService;" = "import payment.service.com.aerionsoft.application.SslCommerzService;"
    "import com.example.tufantrip.service.UserService;" = "import user.service.com.aerionsoft.application.UserService;"
    "import com.example.tufantrip.service.UserCoordinatorService;" = "import user.service.com.aerionsoft.application.UserCoordinatorService;"
    "import com.example.tufantrip.service.UserCleanupService;" = "import user.service.com.aerionsoft.application.UserCleanupService;"
    "import com.example.tufantrip.service.UserDetailsServiceImpl;" = "import user.service.com.aerionsoft.application.UserDetailsServiceImpl;"
    "import com.example.tufantrip.service.CustomUserDetails;" = "import user.service.com.aerionsoft.application.CustomUserDetails;"
    "import com.example.tufantrip.service.BusinessService;" = "import business.service.com.aerionsoft.application.BusinessService;"
    "import com.example.tufantrip.service.BusinessServiceImpl;" = "import business.service.com.aerionsoft.application.BusinessServiceImpl;"
    "import com.example.tufantrip.service.BusinessProviderService;" = "import business.service.com.aerionsoft.application.BusinessProviderService;"
    "import com.example.tufantrip.service.BusinessProviderServiceImpl;" = "import business.service.com.aerionsoft.application.BusinessProviderServiceImpl;"
    "import com.example.tufantrip.service.BusinessSalesPersonService;" = "import business.service.com.aerionsoft.application.BusinessSalesPersonService;"
    "import com.example.tufantrip.service.BusinessSalesPersonServiceImpl;" = "import business.service.com.aerionsoft.application.BusinessSalesPersonServiceImpl;"
    "import com.example.tufantrip.service.SalesPersonService;" = "import business.service.com.aerionsoft.application.SalesPersonService;"
    "import com.example.tufantrip.service.CountriesService;" = "import common.service.com.aerionsoft.application.CountriesService;"
    "import com.example.tufantrip.service.CurrencyService;" = "import common.service.com.aerionsoft.application.CurrencyService;"
    "import com.example.tufantrip.service.FileStorageService;" = "import common.service.com.aerionsoft.application.FileStorageService;"
    "import com.example.tufantrip.service.GlobalSearchService;" = "import common.service.com.aerionsoft.application.GlobalSearchService;"
    "com.example.tufantrip.service.CustomUserDetails" = "user.service.com.aerionsoft.application.CustomUserDetails"
}

Get-ChildItem -Path "src" -Recurse -Filter "*.java" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $original = $content
    foreach ($entry in $replacements.GetEnumerator()) {
        $content = $content.Replace($entry.Key, $entry.Value)
    }
    if ($content -ne $original) {
        Set-Content $_.FullName $content -NoNewline
    }
}

Write-Host "Package and import updates complete."
