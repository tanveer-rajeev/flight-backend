$ErrorActionPreference = "Stop"
Set-Location "d:\java\flight-backend"

# Fix payment package
$f = "src/main/java/com/example/tufantrip/service/payment/SslCommerzService.java"
if (Test-Path $f) {
    $c = Get-Content $f -Raw
    $c = $c -replace '(?m)^package\s+com\.example\.tufantrip\.service;', 'package com.example.tufantrip.service.payment;'
    Set-Content $f $c -NoNewline
}

$moreReplacements = [ordered]@{
    "import com.example.tufantrip.service.PlatformProviderService;" = "import common.service.com.aerionsoft.application.PlatformProviderService;"
    "import com.example.tufantrip.service.AgentIdGenerator;" = "import common.service.com.aerionsoft.application.AgentIdGenerator;"
    "import com.example.tufantrip.service.EmailService;" = "import common.service.com.aerionsoft.application.EmailService;"
    "import com.example.tufantrip.service.RoleService;" = "import com.example.tufantrip.service.RoleService;"
}

# Move a few more shared services to common
$extraMoves = @{
    "src/main/java/com/example/tufantrip/service/PlatformProviderService.java" = "src/main/java/com/example/tufantrip/service/common/PlatformProviderService.java"
    "src/main/java/com/example/tufantrip/service/AgentIdGenerator.java" = "src/main/java/com/example/tufantrip/service/common/AgentIdGenerator.java"
    "src/main/java/com/example/tufantrip/service/EmailService.java" = "src/main/java/com/example/tufantrip/service/common/EmailService.java"
}
foreach ($entry in $extraMoves.GetEnumerator()) {
    if (Test-Path $entry.Key) { git mv $entry.Key $entry.Value }
}

# Update package for new common services
@("PlatformProviderService", "AgentIdGenerator", "EmailService") | ForEach-Object {
    $path = "src/main/java/com/example/tufantrip/service/common/$_.java"
    if (Test-Path $path) {
        $c = Get-Content $path -Raw
        $c = $c -replace '(?m)^package\s+com\.example\.tufantrip\.service;', 'package com.example.tufantrip.service.common;'
        Set-Content $path $c -NoNewline
    }
}

$moreReplacements["import com.example.tufantrip.service.PlatformProviderService;"] = "import common.service.com.aerionsoft.application.PlatformProviderService;"
$moreReplacements["import com.example.tufantrip.service.AgentIdGenerator;"] = "import common.service.com.aerionsoft.application.AgentIdGenerator;"
$moreReplacements["import com.example.tufantrip.service.EmailService;"] = "import common.service.com.aerionsoft.application.EmailService;"

Get-ChildItem -Path "src" -Recurse -Filter "*.java" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $original = $content
    foreach ($entry in $moreReplacements.GetEnumerator()) {
        $content = $content.Replace($entry.Key, $entry.Value)
    }
    if ($content -ne $original) { Set-Content $_.FullName $content -NoNewline }
}

# Add BaseController import where missing in domain controllers
Get-ChildItem "src/main/java/com/example/tufantrip/controller" -Recurse -Filter "*.java" |
    Where-Object { $_.DirectoryName -notmatch "\\controller$" -and $_.Name -ne "BaseController.java" -and $_.Name -ne "package-info.java" } |
    ForEach-Object {
        $content = Get-Content $_.FullName -Raw
        if ($content -match "extends BaseController" -and $content -notmatch "import com\.example\.tufantrip\.controller\.BaseController") {
            $content = $content -replace "(?m)^package .+;\r?\n", "`$0`nimport controller.com.aerionsoft.application.BaseController;`n"
            Set-Content $_.FullName $content -NoNewline
        }
    }

# Cross-domain imports for booking package
$bookingImports = @"
import wallet.service.com.aerionsoft.application.ReferenceGeneratorService;
import common.service.com.aerionsoft.application.CurrencyService;
import com.example.tufantrip.service.ErrorLogService;
import business.service.com.aerionsoft.application.BusinessService;
import wallet.service.com.aerionsoft.application.CreditLimitValidatorService;
import common.service.com.aerionsoft.application.PlatformProviderService;
import user.service.com.aerionsoft.application.UserService;
"@

$bookingFiles = @(
    "BookingCoordinatorService.java",
    "BookingService.java",
    "BookingPriceService.java",
    "TicketActionRequestService.java"
)
foreach ($name in $bookingFiles) {
    $path = "src/main/java/com/example/tufantrip/service/booking/$name"
    if (-not (Test-Path $path)) { continue }
    $content = Get-Content $path -Raw
    foreach ($line in ($bookingImports -split "`n")) {
        $line = $line.Trim()
        if ($line -and $content -notmatch [regex]::Escape($line)) {
            $content = $content -replace "(?m)^package com\.example\.tufantrip\.service\.booking;\r?\n", "`$0$line`n"
        }
    }
    Set-Content $path $content -NoNewline
}

# Wallet cross imports
$walletFiles = @("WalletService.java", "CreditLimitValidatorService.java", "CreditLimitServiceImpl.java")
foreach ($name in $walletFiles) {
    $path = "src/main/java/com/example/tufantrip/service/wallet/$name"
    if (-not (Test-Path $path)) { continue }
    $content = Get-Content $path -Raw
    foreach ($line in @(
        "import common.service.com.aerionsoft.application.CurrencyService;",
        "import business.service.com.aerionsoft.application.BusinessService;"
    )) {
        if ($content -notmatch [regex]::Escape($line)) {
            $content = $content -replace "(?m)^package com\.example\.tufantrip\.service\.wallet;\r?\n", "`$0$line`n"
        }
    }
    Set-Content $path $content -NoNewline
}

# Payment SslCommerz
$path = "src/main/java/com/example/tufantrip/service/payment/SslCommerzService.java"
if (Test-Path $path) {
    $content = Get-Content $path -Raw
    foreach ($line in @(
        "import wallet.service.com.aerionsoft.application.WalletService;",
        "import wallet.service.com.aerionsoft.application.ReferenceGeneratorService;",
        "import common.service.com.aerionsoft.application.CurrencyService;"
    )) {
        if ($content -notmatch [regex]::Escape($line)) {
            $content = $content -replace "(?m)^package com\.example\.tufantrip\.service\.payment;\r?\n", "`$0$line`n"
        }
    }
    Set-Content $path $content -NoNewline
}

# User service cross imports
$path = "src/main/java/com/example/tufantrip/service/user/UserService.java"
if (Test-Path $path) {
    $content = Get-Content $path -Raw
    foreach ($line in @(
        "import common.service.com.aerionsoft.application.FileStorageService;",
        "import com.example.tufantrip.service.RoleService;",
        "import common.service.com.aerionsoft.application.AgentIdGenerator;",
        "import common.service.com.aerionsoft.application.CurrencyService;",
        "import wallet.service.com.aerionsoft.application.CreditLimitValidatorService;",
        "import wallet.service.com.aerionsoft.application.CreditLimitService;",
        "import common.service.com.aerionsoft.application.EmailService;",
        "import business.service.com.aerionsoft.application.BusinessService;"
    )) {
        if ($content -notmatch [regex]::Escape($line)) {
            $content = $content -replace "(?m)^package com\.example\.tufantrip\.service\.user;\r?\n", "`$0$line`n"
        }
    }
    Set-Content $path $content -NoNewline
}

# UserCoordinator
$path = "src/main/java/com/example/tufantrip/service/user/UserCoordinatorService.java"
if (Test-Path $path) {
    $content = Get-Content $path -Raw
    $line = "import booking.service.com.aerionsoft.application.TravellerService;"
    if ($content -notmatch [regex]::Escape($line)) {
        $content = $content -replace "(?m)^package com\.example\.tufantrip\.service\.user;\r?\n", "`$0$line`n"
        Set-Content $path $content -NoNewline
    }
}

# BusinessServiceImpl
$path = "src/main/java/com/example/tufantrip/service/business/BusinessServiceImpl.java"
if (Test-Path $path) {
    $content = Get-Content $path -Raw
    foreach ($line in @(
        "import com.example.tufantrip.service.RoleService;",
        "import common.service.com.aerionsoft.application.AgentIdGenerator;",
        "import common.service.com.aerionsoft.application.CurrencyService;",
        "import wallet.service.com.aerionsoft.application.WalletService;"
    )) {
        if ($content -notmatch [regex]::Escape($line)) {
            $content = $content -replace "(?m)^package com\.example\.tufantrip\.service\.business;\r?\n", "`$0$line`n"
        }
    }
    Set-Content $path $content -NoNewline
}

# SalesPersonService
$path = "src/main/java/com/example/tufantrip/service/business/SalesPersonService.java"
if (Test-Path $path) {
    $content = Get-Content $path -Raw
    $line = "import com.example.tufantrip.service.RoleService;"
    if ($content -notmatch [regex]::Escape($line)) {
        $content = $content -replace "(?m)^package com\.example\.tufantrip\.service\.business;\r?\n", "`$0$line`n"
        Set-Content $path $content -NoNewline
    }
}

# SummeryService
$path = "src/main/java/com/example/tufantrip/service/SummeryService.java"
if (Test-Path $path) {
    $content = Get-Content $path -Raw
    foreach ($line in @(
        "import wallet.service.com.aerionsoft.application.WalletService;",
        "import user.service.com.aerionsoft.application.UserService;",
        "import booking.service.com.aerionsoft.application.BookingService;"
    )) {
        if ($content -notmatch [regex]::Escape($line)) {
            $content = $content -replace "(?m)^package com\.example\.tufantrip\.service;\r?\n", "`$0$line`n"
        }
    }
    Set-Content $path $content -NoNewline
}

# ActiveUserPresenceService
$path = "src/main/java/com/example/tufantrip/service/ActiveUserPresenceService.java"
if (Test-Path $path) {
    $content = Get-Content $path -Raw
    $line = "import user.service.com.aerionsoft.application.CustomUserDetails;"
    if ($content -notmatch [regex]::Escape($line)) {
        $content = $content -replace "(?m)^package com\.example\.tufantrip\.service;\r?\n", "`$0$line`n"
        Set-Content $path $content -NoNewline
    }
}

Write-Host "Fix pass complete."
