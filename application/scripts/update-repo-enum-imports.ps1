$ErrorActionPreference = "Stop"
Set-Location "d:\java\flight-backend"

$repoReplacements = [ordered]@{
    "import com.example.tufantrip.repository.rolePermission." = "import com.example.tufantrip.repository.access."
    "import com.example.tufantrip.repository.TransactionRepository;" = "import wallet.repository.com.aerionsoft.application.TransactionRepository;"
    "import com.example.tufantrip.repository.WalletDepositRepository;" = "import wallet.repository.com.aerionsoft.application.WalletDepositRepository;"
    "import com.example.tufantrip.repository.BalanceChangeHistoryRepository;" = "import wallet.repository.com.aerionsoft.application.BalanceChangeHistoryRepository;"
    "import com.example.tufantrip.repository.DepositBankRepository;" = "import wallet.repository.com.aerionsoft.application.DepositBankRepository;"
    "import com.example.tufantrip.repository.CreditLimitHistoryRepository;" = "import wallet.repository.com.aerionsoft.application.CreditLimitHistoryRepository;"
    "import com.example.tufantrip.repository.CreditRequestRepository;" = "import wallet.repository.com.aerionsoft.application.CreditRequestRepository;"
    "import com.example.tufantrip.repository.WalletDepositPaymentRepository;" = "import wallet.repository.com.aerionsoft.application.WalletDepositPaymentRepository;"
    "import com.example.tufantrip.repository.WalletReferenceSequenceRepository;" = "import wallet.repository.com.aerionsoft.application.WalletReferenceSequenceRepository;"
    "import com.example.tufantrip.repository.WalletDepositSpec;" = "import wallet.repository.com.aerionsoft.application.WalletDepositSpec;"
    "import com.example.tufantrip.repository.UserBalanceReconciliationRepository;" = "import wallet.repository.com.aerionsoft.application.UserBalanceReconciliationRepository;"
    "import com.example.tufantrip.repository.TravellerRepository;" = "import booking.repository.com.aerionsoft.application.TravellerRepository;"
    "import com.example.tufantrip.repository.TravelInformationRepository;" = "import booking.repository.com.aerionsoft.application.TravelInformationRepository;"
    "import com.example.tufantrip.repository.BookingSegmentRepository;" = "import booking.repository.com.aerionsoft.application.BookingSegmentRepository;"
    "import com.example.tufantrip.repository.TicketActionRequestRepository;" = "import booking.repository.com.aerionsoft.application.TicketActionRequestRepository;"
    "import com.example.tufantrip.repository.SegmentAirlineRepository;" = "import booking.repository.com.aerionsoft.application.SegmentAirlineRepository;"
    "import com.example.tufantrip.repository.SegmentAirportRepository;" = "import booking.repository.com.aerionsoft.application.SegmentAirportRepository;"
    "import com.example.tufantrip.repository.PaymentRepository;" = "import payment.repository.com.aerionsoft.application.PaymentRepository;"
    "import com.example.tufantrip.repository.SslCommerzPaymentRepository;" = "import payment.repository.com.aerionsoft.application.SslCommerzPaymentRepository;"
    "import com.example.tufantrip.repository.StripeCredRepository;" = "import payment.repository.com.aerionsoft.application.StripeCredRepository;"
    "import com.example.tufantrip.repository.ExpenseRepository;" = "import expense.repository.com.aerionsoft.application.ExpenseRepository;"
    "import com.example.tufantrip.repository.ExpenseDetailRepository;" = "import expense.repository.com.aerionsoft.application.ExpenseDetailRepository;"
    "import com.example.tufantrip.repository.AccountHeadRepository;" = "import finance.repository.com.aerionsoft.application.AccountHeadRepository;"
    "import com.example.tufantrip.repository.BusinessRepository;" = "import business.repository.com.aerionsoft.application.BusinessRepository;"
    "import com.example.tufantrip.repository.BusinessProviderRepository;" = "import business.repository.com.aerionsoft.application.BusinessProviderRepository;"
    "import com.example.tufantrip.repository.BusinessSalesPersonRepository;" = "import business.repository.com.aerionsoft.application.BusinessSalesPersonRepository;"
    "import com.example.tufantrip.repository.UserRepository;" = "import user.repository.com.aerionsoft.application.UserRepository;"
    "import com.example.tufantrip.repository.AdminUserRepository;" = "import user.repository.com.aerionsoft.application.AdminUserRepository;"
    "import com.example.tufantrip.repository.LoginHistoryRepository;" = "import user.repository.com.aerionsoft.application.LoginHistoryRepository;"
    "import com.example.tufantrip.repository.RefreshTokenRepository;" = "import user.repository.com.aerionsoft.application.RefreshTokenRepository;"
    "import com.example.tufantrip.repository.AgentIdSequenceRepository;" = "import user.repository.com.aerionsoft.application.AgentIdSequenceRepository;"
    "import com.example.tufantrip.repository.BreakingNewsRepository;" = "import breakingnews.repository.com.aerionsoft.application.BreakingNewsRepository;"
    "import com.example.tufantrip.repository.NotificationRepository;" = "import notification.repository.com.aerionsoft.application.NotificationRepository;"
    "import com.example.tufantrip.repository.NotificationTemplateRepository;" = "import notification.repository.com.aerionsoft.application.NotificationTemplateRepository;"
    "import com.example.tufantrip.repository.NotificationPreferenceRepository;" = "import notification.repository.com.aerionsoft.application.NotificationPreferenceRepository;"
    "import com.example.tufantrip.repository.NotificationDeliveryLogRepository;" = "import notification.repository.com.aerionsoft.application.NotificationDeliveryLogRepository;"
    "import com.example.tufantrip.repository.MarkupRuleRepository;" = "import flight.repository.com.aerionsoft.application.MarkupRuleRepository;"
    "import com.example.tufantrip.repository.MarkupPlanRepository;" = "import flight.repository.com.aerionsoft.application.MarkupPlanRepository;"
    "import com.example.tufantrip.repository.MarkupPlanBusinessRepository;" = "import flight.repository.com.aerionsoft.application.MarkupPlanBusinessRepository;"
    "import com.example.tufantrip.repository.MarkupLogRepository;" = "import flight.repository.com.aerionsoft.application.MarkupLogRepository;"
    "import com.example.tufantrip.repository.MealRepository;" = "import flight.repository.com.aerionsoft.application.MealRepository;"
    "import com.example.tufantrip.repository.AccommodationRepository;" = "import flight.repository.com.aerionsoft.application.AccommodationRepository;"
    "import com.example.tufantrip.repository.FlightSearchLogRepository;" = "import flight.repository.com.aerionsoft.application.FlightSearchLogRepository;"
    "import com.example.tufantrip.repository.DailyTicketSegmentRepository;" = "import flight.repository.com.aerionsoft.application.DailyTicketSegmentRepository;"
    "import com.example.tufantrip.repository.ContentRepository;" = "import cms.repository.com.aerionsoft.application.ContentRepository;"
    "import com.example.tufantrip.repository.ContentTagMapRepository;" = "import cms.repository.com.aerionsoft.application.ContentTagMapRepository;"
    "import com.example.tufantrip.repository.ContentCategoryMapRepository;" = "import cms.repository.com.aerionsoft.application.ContentCategoryMapRepository;"
    "import com.example.tufantrip.repository.ContentSectionMapRepository;" = "import cms.repository.com.aerionsoft.application.ContentSectionMapRepository;"
    "import com.example.tufantrip.repository.CategoryRepository;" = "import cms.repository.com.aerionsoft.application.CategoryRepository;"
    "import com.example.tufantrip.repository.SectionRepository;" = "import cms.repository.com.aerionsoft.application.SectionRepository;"
    "import com.example.tufantrip.repository.TagRepository;" = "import cms.repository.com.aerionsoft.application.TagRepository;"
    "import com.example.tufantrip.repository.MediaRepository;" = "import cms.repository.com.aerionsoft.application.MediaRepository;"
    "import com.example.tufantrip.repository.CustomFormRepository;" = "import cms.repository.com.aerionsoft.application.CustomFormRepository;"
    "import com.example.tufantrip.repository.PackageItemRepository;" = "import cms.repository.com.aerionsoft.application.PackageItemRepository;"
    "import com.example.tufantrip.repository.CountriesRepository;" = "import common.repository.com.aerionsoft.application.CountriesRepository;"
    "import com.example.tufantrip.repository.CurrencyRepository;" = "import common.repository.com.aerionsoft.application.CurrencyRepository;"
    "import com.example.tufantrip.repository.TourApplicationRepository;" = "import tour.repository.com.aerionsoft.application.TourApplicationRepository;"
    "import com.example.tufantrip.repository.VisaInfoRepository;" = "import visa.repository.com.aerionsoft.application.VisaInfoRepository;"
}

$enumReplacements = [ordered]@{
    "import com.example.tufantrip.enums.DepositType;" = "import wallet.enums.com.aerionsoft.application.DepositType;"
    "import com.example.tufantrip.enums.DepositStatus;" = "import wallet.enums.com.aerionsoft.application.DepositStatus;"
    "import com.example.tufantrip.enums.TransactionStatus;" = "import wallet.enums.com.aerionsoft.application.TransactionStatus;"
    "import com.example.tufantrip.enums.TransactionSourceType;" = "import wallet.enums.com.aerionsoft.application.TransactionSourceType;"
    "import com.example.tufantrip.enums.CreditLimitStatus;" = "import wallet.enums.com.aerionsoft.application.CreditLimitStatus;"
    "import com.example.tufantrip.enums.CreditRequestStatus;" = "import wallet.enums.com.aerionsoft.application.CreditRequestStatus;"
    "import com.example.tufantrip.enums.PaymentMethod;" = "import wallet.enums.com.aerionsoft.application.PaymentProvider;"
    "import com.example.tufantrip.enums.PaymentType;" = "import wallet.enums.com.aerionsoft.application.PaymentType;"
    "import com.example.tufantrip.enums.BookingStatus;" = "import booking.enums.com.aerionsoft.application.BookingStatus;"
    "import com.example.tufantrip.enums.BookingType;" = "import booking.enums.com.aerionsoft.application.BookingType;"
    "import com.example.tufantrip.enums.BookingClass;" = "import booking.enums.com.aerionsoft.application.BookingClass;"
    "import com.example.tufantrip.enums.BookType;" = "import booking.enums.com.aerionsoft.application.BookType;"
    "import com.example.tufantrip.enums.TicketActionStatus;" = "import booking.enums.com.aerionsoft.application.TicketActionStatus;"
    "import com.example.tufantrip.enums.TicketActionType;" = "import booking.enums.com.aerionsoft.application.TicketActionType;"
    "import com.example.tufantrip.enums.TripType;" = "import booking.enums.com.aerionsoft.application.TripType;"
    "import com.example.tufantrip.enums.SearchType;" = "import booking.enums.com.aerionsoft.application.SearchType;"
    "import com.example.tufantrip.enums.Provider;" = "import booking.enums.com.aerionsoft.application.Provider;"
    "import com.example.tufantrip.enums.PassenserEnum;" = "import booking.enums.com.aerionsoft.application.PassenserEnum;"
    "import com.example.tufantrip.enums.ServiceProviderEnum;" = "import booking.enums.com.aerionsoft.application.ServiceProviderEnum;"
    "import com.example.tufantrip.enums.ApplicationStatus;" = "import tour.enums.com.aerionsoft.application.ApplicationStatus;"
    "import com.example.tufantrip.enums.ExpenseStatus;" = "import expense.enums.com.aerionsoft.application.ExpenseStatus;"
    "import com.example.tufantrip.enums.AccountHeadType;" = "import finance.enums.com.aerionsoft.application.AccountHeadType;"
    "import com.example.tufantrip.enums.BusinessStatus;" = "import business.enums.com.aerionsoft.application.BusinessStatus;"
    "import com.example.tufantrip.enums.Role;" = "import access.enums.com.aerionsoft.application.Role;"
    "import com.example.tufantrip.enums.PermissionType;" = "import access.enums.com.aerionsoft.application.PermissionType;"
    "import com.example.tufantrip.enums.PermissionModule;" = "import access.enums.com.aerionsoft.application.PermissionModule;"
    "import com.example.tufantrip.enums.UserType;" = "import user.enums.com.aerionsoft.application.UserType;"
    "import com.example.tufantrip.enums.Gender;" = "import user.enums.com.aerionsoft.application.Gender;"
    "import com.example.tufantrip.enums.NotificationPriority;" = "import notification.enums.com.aerionsoft.application.NotificationPriority;"
    "import com.example.tufantrip.enums.NotificationStatus;" = "import notification.enums.com.aerionsoft.application.NotificationStatus;"
    "import com.example.tufantrip.enums.NotificationType;" = "import notification.enums.com.aerionsoft.application.NotificationType;"
    "import com.example.tufantrip.enums.DeliveryChannel;" = "import notification.enums.com.aerionsoft.application.DeliveryChannel;"
    "import com.example.tufantrip.enums.DeliveryStatus;" = "import notification.enums.com.aerionsoft.application.DeliveryStatus;"
    "import com.example.tufantrip.enums.BreakingNewsTarget;" = "import breakingnews.enums.com.aerionsoft.application.BreakingNewsTarget;"
    "import com.example.tufantrip.enums.InvoiceStatus;" = "import client.enums.com.aerionsoft.application.InvoiceStatus;"
    "import com.example.tufantrip.enums.InvoiceType;" = "import client.enums.com.aerionsoft.application.InvoiceType;"
    "import com.example.tufantrip.enums.ManualInvoicePaymentType;" = "import client.enums.com.aerionsoft.application.ManualInvoicePaymentType;"
    "import com.example.tufantrip.enums.ContentStatus;" = "import cms.enums.com.aerionsoft.application.ContentStatus;"
    "import com.example.tufantrip.enums.ContentType;" = "import cms.enums.com.aerionsoft.application.ContentType;"
    "import com.example.tufantrip.enums.Currency;" = "import common.enums.com.aerionsoft.application.Currency;"
    "import com.example.tufantrip.enums.ErrorCode;" = "import common.enums.com.aerionsoft.application.ErrorCode;"
    "import com.example.tufantrip.enums.UsingPortal;" = "import common.enums.com.aerionsoft.application.UsingPortal;"
    "import com.example.tufantrip.enums.MicroserviceType;" = "import common.enums.com.aerionsoft.application.MicroserviceType;"
    "import com.example.tufantrip.enums.NGeniusActionType;" = "import common.enums.com.aerionsoft.application.NGeniusActionType;"
}

# Fix enum imports inside moved repository files
$repoEnumFixes = [ordered]@{
    "import com.example.tufantrip.enums.BusinessStatus;" = "import business.enums.com.aerionsoft.application.BusinessStatus;"
    "import com.example.tufantrip.enums.ExpenseStatus;" = "import expense.enums.com.aerionsoft.application.ExpenseStatus;"
    "import com.example.tufantrip.enums.DepositStatus;" = "import wallet.enums.com.aerionsoft.application.DepositStatus;"
    "import com.example.tufantrip.enums.DepositType;" = "import wallet.enums.com.aerionsoft.application.DepositType;"
    "import com.example.tufantrip.enums.TransactionStatus;" = "import wallet.enums.com.aerionsoft.application.TransactionStatus;"
    "import com.example.tufantrip.enums.BookingStatus;" = "import booking.enums.com.aerionsoft.application.BookingStatus;"
    "import com.example.tufantrip.enums.NotificationStatus;" = "import notification.enums.com.aerionsoft.application.NotificationStatus;"
    "import com.example.tufantrip.enums.DeliveryStatus;" = "import notification.enums.com.aerionsoft.application.DeliveryStatus;"
    "import com.example.tufantrip.enums.ContentStatus;" = "import cms.enums.com.aerionsoft.application.ContentStatus;"
    "import com.example.tufantrip.enums.ContentType;" = "import cms.enums.com.aerionsoft.application.ContentType;"
    "import com.example.tufantrip.enums.ApplicationStatus;" = "import tour.enums.com.aerionsoft.application.ApplicationStatus;"
    "import com.example.tufantrip.enums.PermissionModule;" = "import access.enums.com.aerionsoft.application.PermissionModule;"
    "import com.example.tufantrip.enums.PermissionType;" = "import access.enums.com.aerionsoft.application.PermissionType;"
    "import com.example.tufantrip.enums.InvoiceStatus;" = "import client.enums.com.aerionsoft.application.InvoiceStatus;"
    "import com.example.tufantrip.enums.BreakingNewsTarget;" = "import breakingnews.enums.com.aerionsoft.application.BreakingNewsTarget;"
}

$allReplacements = @{}
foreach ($entry in $repoReplacements.GetEnumerator()) { $allReplacements[$entry.Key] = $entry.Value }
foreach ($entry in $enumReplacements.GetEnumerator()) { $allReplacements[$entry.Key] = $entry.Value }

$changed = 0
Get-ChildItem -Path "src" -Recurse -Filter "*.java" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $original = $content
    foreach ($entry in $allReplacements.GetEnumerator()) {
        $content = $content.Replace($entry.Key, $entry.Value)
    }
    if ($_.FullName -match '\\repository\\') {
        foreach ($entry in $repoEnumFixes.GetEnumerator()) {
            $content = $content.Replace($entry.Key, $entry.Value)
        }
    }
    if ($content -ne $original) {
        Set-Content $_.FullName $content -NoNewline
        $changed++
    }
}

Write-Host "Updated imports in $changed files."
