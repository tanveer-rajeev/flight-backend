$ErrorActionPreference = "Stop"
Set-Location "d:\java\flight-backend"

# --- Create domain folders ---
$repoDirs = @("wallet","booking","payment","expense","finance","business","user","breakingnews","notification","flight","cms","access")
$enumDirs = @("wallet","booking","tour","expense","finance","business","user","access","notification","breakingnews","client","cms","common","flight")
foreach ($d in $repoDirs) { New-Item -ItemType Directory -Force -Path "src/main/java/com/example/tufantrip/repository/$d" | Out-Null }
foreach ($d in $enumDirs) { New-Item -ItemType Directory -Force -Path "src/main/java/com/example/tufantrip/enums/$d" | Out-Null }

function Move-Repo($file, $domain) {
    $from = "src/main/java/com/example/tufantrip/repository/$file"
    $to = "src/main/java/com/example/tufantrip/repository/$domain/$file"
    if (Test-Path $from) { git mv $from $to }
}

# --- Repository moves ---
@(
    @("TransactionRepository.java","wallet"),
    @("WalletDepositRepository.java","wallet"),
    @("BalanceChangeHistoryRepository.java","wallet"),
    @("DepositBankRepository.java","wallet"),
    @("CreditLimitHistoryRepository.java","wallet"),
    @("CreditRequestRepository.java","wallet"),
    @("WalletDepositPaymentRepository.java","wallet"),
    @("WalletReferenceSequenceRepository.java","wallet"),
    @("WalletDepositSpec.java","wallet"),
    @("UserBalanceReconciliationRepository.java","wallet"),
    @("TravellerRepository.java","booking"),
    @("TravelInformationRepository.java","booking"),
    @("BookingSegmentRepository.java","booking"),
    @("TicketActionRequestRepository.java","booking"),
    @("SegmentAirlineRepository.java","booking"),
    @("SegmentAirportRepository.java","booking"),
    @("PaymentRepository.java","payment"),
    @("SslCommerzPaymentRepository.java","payment"),
    @("StripeCredRepository.java","payment"),
    @("ExpenseRepository.java","expense"),
    @("ExpenseDetailRepository.java","expense"),
    @("AccountHeadRepository.java","finance"),
    @("BusinessRepository.java","business"),
    @("BusinessProviderRepository.java","business"),
    @("BusinessSalesPersonRepository.java","business"),
    @("UserRepository.java","user"),
    @("AdminUserRepository.java","user"),
    @("LoginHistoryRepository.java","user"),
    @("RefreshTokenRepository.java","user"),
    @("AgentIdSequenceRepository.java","user"),
    @("BreakingNewsRepository.java","breakingnews"),
    @("NotificationRepository.java","notification"),
    @("NotificationTemplateRepository.java","notification"),
    @("NotificationPreferenceRepository.java","notification"),
    @("NotificationDeliveryLogRepository.java","notification"),
    @("MarkupRuleRepository.java","flight"),
    @("MarkupPlanRepository.java","flight"),
    @("MarkupPlanBusinessRepository.java","flight"),
    @("MarkupLogRepository.java","flight"),
    @("MealRepository.java","flight"),
    @("AccommodationRepository.java","flight"),
    @("FlightSearchLogRepository.java","flight"),
    @("DailyTicketSegmentRepository.java","flight"),
    @("ContentRepository.java","cms"),
    @("ContentTagMapRepository.java","cms"),
    @("ContentCategoryMapRepository.java","cms"),
    @("ContentSectionMapRepository.java","cms"),
    @("CategoryRepository.java","cms"),
    @("SectionRepository.java","cms"),
    @("TagRepository.java","cms"),
    @("MediaRepository.java","cms"),
    @("CustomFormRepository.java","cms"),
    @("PackageItemRepository.java","cms"),
    @("CountriesRepository.java","common"),
    @("CurrencyRepository.java","common"),
    @("TourApplicationRepository.java","tour"),
    @("VisaInfoRepository.java","visa")
) | ForEach-Object { Move-Repo $_[0] $_[1] }

# rolePermission -> access
if (Test-Path "src/main/java/com/example/tufantrip/repository/rolePermission") {
    Get-ChildItem "src/main/java/com/example/tufantrip/repository/rolePermission" -Filter "*.java" | ForEach-Object {
        git mv $_.FullName "src/main/java/com/example/tufantrip/repository/access/$($_.Name)"
    }
    Remove-Item "src/main/java/com/example/tufantrip/repository/rolePermission" -Force -ErrorAction SilentlyContinue
}

function Move-Enum($file, $domain) {
    $from = "src/main/java/com/example/tufantrip/enums/$file"
    $to = "src/main/java/com/example/tufantrip/enums/$domain/$file"
    if (Test-Path $from) { git mv $from $to }
}

# --- Enum moves ---
@(
    @("DepositType.java","wallet"), @("DepositStatus.java","wallet"), @("TransactionStatus.java","wallet"),
    @("TransactionSourceType.java","wallet"), @("CreditLimitStatus.java","wallet"), @("CreditRequestStatus.java","wallet"),
    @("PaymentMethod.java","wallet"), @("PaymentType.java","wallet"),
    @("BookingStatus.java","booking"), @("BookingType.java","booking"), @("BookingClass.java","booking"),
    @("BookType.java","booking"), @("TicketActionStatus.java","booking"), @("TicketActionType.java","booking"),
    @("TripType.java","booking"), @("SearchType.java","booking"), @("Provider.java","booking"),
    @("PassenserEnum.java","booking"), @("ServiceProviderEnum.java","booking"),
    @("ApplicationStatus.java","tour"),
    @("ExpenseStatus.java","expense"),
    @("AccountHeadType.java","finance"),
    @("BusinessStatus.java","business"),
    @("Role.java","access"), @("PermissionType.java","access"), @("PermissionModule.java","access"),
    @("UserType.java","user"), @("Gender.java","user"),
    @("NotificationPriority.java","notification"), @("NotificationStatus.java","notification"),
    @("NotificationType.java","notification"), @("DeliveryChannel.java","notification"), @("DeliveryStatus.java","notification"),
    @("BreakingNewsTarget.java","breakingnews"),
    @("InvoiceStatus.java","client"), @("InvoiceType.java","client"), @("ManualInvoicePaymentType.java","client"),
    @("ContentStatus.java","cms"), @("ContentType.java","cms"),
    @("Currency.java","common"), @("ErrorCode.java","common"), @("UsingPortal.java","common"),
    @("MicroserviceType.java","common"), @("NGeniusActionType.java","common")
) | ForEach-Object { Move-Enum $_[0] $_[1] }

# --- Update package declarations in moved repo/enum files ---
$allDomains = @{
    "src/main/java/com/example/tufantrip/repository/wallet" = "com.example.tufantrip.repository.wallet"
    "src/main/java/com/example/tufantrip/repository/booking" = "com.example.tufantrip.repository.booking"
    "src/main/java/com/example/tufantrip/repository/payment" = "com.example.tufantrip.repository.payment"
    "src/main/java/com/example/tufantrip/repository/expense" = "com.example.tufantrip.repository.expense"
    "src/main/java/com/example/tufantrip/repository/finance" = "com.example.tufantrip.repository.finance"
    "src/main/java/com/example/tufantrip/repository/business" = "com.example.tufantrip.repository.business"
    "src/main/java/com/example/tufantrip/repository/user" = "com.example.tufantrip.repository.user"
    "src/main/java/com/example/tufantrip/repository/breakingnews" = "com.example.tufantrip.repository.breakingnews"
    "src/main/java/com/example/tufantrip/repository/notification" = "com.example.tufantrip.repository.notification"
    "src/main/java/com/example/tufantrip/repository/flight" = "com.example.tufantrip.repository.flight"
    "src/main/java/com/example/tufantrip/repository/cms" = "com.example.tufantrip.repository.cms"
    "src/main/java/com/example/tufantrip/repository/access" = "com.example.tufantrip.repository.access"
    "src/main/java/com/example/tufantrip/enums/wallet" = "com.example.tufantrip.enums.wallet"
    "src/main/java/com/example/tufantrip/enums/booking" = "com.example.tufantrip.enums.booking"
    "src/main/java/com/example/tufantrip/enums/tour" = "com.example.tufantrip.enums.tour"
    "src/main/java/com/example/tufantrip/enums/expense" = "com.example.tufantrip.enums.expense"
    "src/main/java/com/example/tufantrip/enums/finance" = "com.example.tufantrip.enums.finance"
    "src/main/java/com/example/tufantrip/enums/business" = "com.example.tufantrip.enums.business"
    "src/main/java/com/example/tufantrip/enums/user" = "com.example.tufantrip.enums.user"
    "src/main/java/com/example/tufantrip/enums/access" = "com.example.tufantrip.enums.access"
    "src/main/java/com/example/tufantrip/enums/notification" = "com.example.tufantrip.enums.notification"
    "src/main/java/com/example/tufantrip/enums/breakingnews" = "com.example.tufantrip.enums.breakingnews"
    "src/main/java/com/example/tufantrip/enums/client" = "com.example.tufantrip.enums.client"
    "src/main/java/com/example/tufantrip/enums/cms" = "com.example.tufantrip.enums.cms"
    "src/main/java/com/example/tufantrip/enums/common" = "com.example.tufantrip.enums.common"
    "src/main/java/com/example/tufantrip/enums/flight" = "com.example.tufantrip.enums.flight"
}
foreach ($entry in $allDomains.GetEnumerator()) {
    if (-not (Test-Path $entry.Key)) { continue }
    Get-ChildItem $entry.Key -Filter "*.java" | ForEach-Object {
        $c = Get-Content $_.FullName -Raw
        $c = $c -replace '(?m)^package\s+com\.example\.tufantrip\.(repository|enums)(\.\w+)*;', "package $($entry.Value);"
        Set-Content $_.FullName $c -NoNewline
    }
}
# tour repo enum folder for ApplicationStatus - tour enum pkg
if (Test-Path "src/main/java/com/example/tufantrip/enums/tour") {
    Get-ChildItem "src/main/java/com/example/tufantrip/enums/tour" -Filter "*.java" | ForEach-Object {
        $c = Get-Content $_.FullName -Raw
        $c = $c -replace '(?m)^package\s+com\.example\.tufantrip\.enums;', 'package com.example.tufantrip.enums.tour;'
        Set-Content $_.FullName $c -NoNewline
    }
}
# common repo (Countries, Currency) - already in common folder
Get-ChildItem "src/main/java/com/example/tufantrip/repository/common" -Filter "*.java" | ForEach-Object {
    $c = Get-Content $_.FullName -Raw
    if ($c -match '(?m)^package com\.example\.tufantrip\.repository;') {
        $c = $c -replace '(?m)^package com\.example\.tufantrip\.repository;', 'package com.example.tufantrip.repository.common;'
        Set-Content $_.FullName $c -NoNewline
    }
}
# tour repo TourApplicationRepository
if (Test-Path "src/main/java/com/example/tufantrip/repository/tour/TourApplicationRepository.java") {
    $c = Get-Content "src/main/java/com/example/tufantrip/repository/tour/TourApplicationRepository.java" -Raw
    $c = $c -replace '(?m)^package com\.example\.tufantrip\.repository;', 'package com.example.tufantrip.repository.tour;'
    Set-Content "src/main/java/com/example/tufantrip/repository/tour/TourApplicationRepository.java" $c -NoNewline
}
if (Test-Path "src/main/java/com/example/tufantrip/repository/visa/VisaInfoRepository.java") {
    $c = Get-Content "src/main/java/com/example/tufantrip/repository/visa/VisaInfoRepository.java" -Raw
    $c = $c -replace '(?m)^package com\.example\.tufantrip\.repository;', 'package com.example.tufantrip.repository.visa;'
    Set-Content "src/main/java/com/example/tufantrip/repository/visa/VisaInfoRepository.java" $c -NoNewline
}

Write-Host "Moves complete. Root repo files:"
Get-ChildItem "src/main/java/com/example/tufantrip/repository" -File -Name -ErrorAction SilentlyContinue
Write-Host "Root enum files:"
Get-ChildItem "src/main/java/com/example/tufantrip/enums" -File -Name -ErrorAction SilentlyContinue
