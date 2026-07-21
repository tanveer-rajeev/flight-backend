$ErrorActionPreference = "Stop"
Set-Location "d:\java\flight-backend"

$replacements = [ordered]@{
    "com.example.tufantrip.enums.NotificationType" = "notification.enums.com.aerionsoft.application.NotificationType"
    "com.example.tufantrip.enums.NotificationPriority" = "notification.enums.com.aerionsoft.application.NotificationPriority"
    "com.example.tufantrip.enums.NotificationStatus" = "notification.enums.com.aerionsoft.application.NotificationStatus"
    "com.example.tufantrip.enums.DeliveryChannel" = "notification.enums.com.aerionsoft.application.DeliveryChannel"
    "com.example.tufantrip.enums.DeliveryStatus" = "notification.enums.com.aerionsoft.application.DeliveryStatus"
    "com.example.tufantrip.enums.BookingStatus" = "booking.enums.com.aerionsoft.application.BookingStatus"
    "com.example.tufantrip.enums.Provider" = "booking.enums.com.aerionsoft.application.Provider"
    "com.example.tufantrip.enums.DepositType" = "wallet.enums.com.aerionsoft.application.DepositType"
    "com.example.tufantrip.enums.DepositStatus" = "wallet.enums.com.aerionsoft.application.DepositStatus"
    "com.example.tufantrip.enums.TransactionStatus" = "wallet.enums.com.aerionsoft.application.TransactionStatus"
    "com.example.tufantrip.enums.TransactionSourceType" = "wallet.enums.com.aerionsoft.application.TransactionSourceType"
    "com.example.tufantrip.enums.Currency" = "common.enums.com.aerionsoft.application.Currency"
    "com.example.tufantrip.enums.UsingPortal" = "common.enums.com.aerionsoft.application.UsingPortal"
    "com.example.tufantrip.enums.MicroserviceType" = "common.enums.com.aerionsoft.application.MicroserviceType"
    "com.example.tufantrip.enums.ErrorCode" = "common.enums.com.aerionsoft.application.ErrorCode"
    "com.example.tufantrip.enums.AccountHeadType" = "finance.enums.com.aerionsoft.application.AccountHeadType"
    "com.example.tufantrip.enums.ExpenseStatus" = "expense.enums.com.aerionsoft.application.ExpenseStatus"
    "com.example.tufantrip.enums.BusinessStatus" = "business.enums.com.aerionsoft.application.BusinessStatus"
    "com.example.tufantrip.enums.InvoiceStatus" = "client.enums.com.aerionsoft.application.InvoiceStatus"
    "com.example.tufantrip.enums.InvoiceType" = "client.enums.com.aerionsoft.application.InvoiceType"
    "com.example.tufantrip.enums.ContentStatus" = "cms.enums.com.aerionsoft.application.ContentStatus"
    "com.example.tufantrip.enums.ContentType" = "cms.enums.com.aerionsoft.application.ContentType"
    "com.example.tufantrip.enums.TicketActionStatus" = "booking.enums.com.aerionsoft.application.TicketActionStatus"
    "com.example.tufantrip.enums.TicketActionType" = "booking.enums.com.aerionsoft.application.TicketActionType"
    "com.example.tufantrip.enums.BreakingNewsTarget" = "breakingnews.enums.com.aerionsoft.application.BreakingNewsTarget"
    "com.example.tufantrip.enums.ApplicationStatus" = "tour.enums.com.aerionsoft.application.ApplicationStatus"
    "com.example.tufantrip.enums.PermissionModule" = "access.enums.com.aerionsoft.application.PermissionModule"
    "com.example.tufantrip.enums.PermissionType" = "access.enums.com.aerionsoft.application.PermissionType"
    "com.example.tufantrip.enums.CreditLimitStatus" = "wallet.enums.com.aerionsoft.application.CreditLimitStatus"
    "com.example.tufantrip.enums.CreditRequestStatus" = "wallet.enums.com.aerionsoft.application.CreditRequestStatus"
}

$changed = 0
Get-ChildItem -Path "src" -Recurse -Filter "*.java" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $original = $content
    foreach ($entry in $replacements.GetEnumerator()) {
        $content = $content.Replace($entry.Key, $entry.Value)
    }
    if ($content -ne $original) {
        Set-Content $_.FullName $content -NoNewline
        $changed++
    }
}
Write-Host "Fixed FQNs in $changed files."
