$ErrorActionPreference = "Stop"
Set-Location "d:\java\flight-backend"

$dirs = @(
    "src/main/java/com/example/tufantrip/service/access",
    "src/main/java/com/example/tufantrip/service/errorlog",
    "src/main/java/com/example/tufantrip/service/breakingnews"
)
foreach ($d in $dirs) { New-Item -ItemType Directory -Force -Path $d | Out-Null }

function Move-File($from, $to) {
    if (Test-Path $from) { git mv $from $to }
}

# access (roles & permissions)
Move-File "src/main/java/com/example/tufantrip/service/PermissionService.java" "src/main/java/com/example/tufantrip/service/access/PermissionService.java"
Move-File "src/main/java/com/example/tufantrip/service/PermissionGroupService.java" "src/main/java/com/example/tufantrip/service/access/PermissionGroupService.java"
Move-File "src/main/java/com/example/tufantrip/service/RoleService.java" "src/main/java/com/example/tufantrip/service/access/RoleService.java"

# error logging
Move-File "src/main/java/com/example/tufantrip/service/ErrorLogService.java" "src/main/java/com/example/tufantrip/service/errorlog/ErrorLogService.java"
Move-File "src/main/java/com/example/tufantrip/service/ErrorLogPersistenceService.java" "src/main/java/com/example/tufantrip/service/errorlog/ErrorLogPersistenceService.java"
Move-File "src/main/java/com/example/tufantrip/service/ErrorLogPartitionService.java" "src/main/java/com/example/tufantrip/service/errorlog/ErrorLogPartitionService.java"
Move-File "src/main/java/com/example/tufantrip/service/ErrorCodeMappingService.java" "src/main/java/com/example/tufantrip/service/errorlog/ErrorCodeMappingService.java"
Move-File "src/main/java/com/example/tufantrip/service/MicroserviceErrorHandler.java" "src/main/java/com/example/tufantrip/service/errorlog/MicroserviceErrorHandler.java"

# breaking news
Move-File "src/main/java/com/example/tufantrip/service/BreakingNewsService.java" "src/main/java/com/example/tufantrip/service/breakingnews/BreakingNewsService.java"
Move-File "src/main/java/com/example/tufantrip/service/BreakingNewsServiceImpl.java" "src/main/java/com/example/tufantrip/service/breakingnews/BreakingNewsServiceImpl.java"

# user presence & storage
Move-File "src/main/java/com/example/tufantrip/service/ActiveUserPresenceService.java" "src/main/java/com/example/tufantrip/service/user/ActiveUserPresenceService.java"
Move-File "src/main/java/com/example/tufantrip/service/S3FileService.java" "src/main/java/com/example/tufantrip/service/common/S3FileService.java"

# report & admin summary
Move-File "src/main/java/com/example/tufantrip/service/DailyReportService.java" "src/main/java/com/example/tufantrip/service/report/DailyReportService.java"
Move-File "src/main/java/com/example/tufantrip/service/SummeryService.java" "src/main/java/com/example/tufantrip/service/admin/SummeryService.java"

# Remove empty transaction folder if present
if (Test-Path "src/main/java/com/example/tufantrip/service/transaction") {
    $remaining = Get-ChildItem "src/main/java/com/example/tufantrip/service/transaction" -ErrorAction SilentlyContinue
    if (-not $remaining) { Remove-Item "src/main/java/com/example/tufantrip/service/transaction" -Force -ErrorAction SilentlyContinue }
}

# Update package declarations
$packageMap = @{
    "src/main/java/com/example/tufantrip/service/access"       = "com.example.tufantrip.service.access"
    "src/main/java/com/example/tufantrip/service/errorlog"     = "com.example.tufantrip.service.errorlog"
    "src/main/java/com/example/tufantrip/service/breakingnews" = "com.example.tufantrip.service.breakingnews"
}
foreach ($entry in $packageMap.GetEnumerator()) {
    if (-not (Test-Path $entry.Key)) { continue }
    Get-ChildItem $entry.Key -Filter "*.java" | Where-Object { $_.Name -ne "package-info.java" } | ForEach-Object {
        $content = Get-Content $_.FullName -Raw
        $content = $content -replace '(?m)^package\s+com\.example\.tufantrip\.service;', "package $($entry.Value);"
        Set-Content $_.FullName $content -NoNewline
    }
}
# user, common, report, admin targets for single files
@(
    @{ Path = "src/main/java/com/example/tufantrip/service/user/ActiveUserPresenceService.java"; Pkg = "com.example.tufantrip.service.user" },
    @{ Path = "src/main/java/com/example/tufantrip/service/common/S3FileService.java"; Pkg = "com.example.tufantrip.service.common" },
    @{ Path = "src/main/java/com/example/tufantrip/service/report/DailyReportService.java"; Pkg = "com.example.tufantrip.service.report" },
    @{ Path = "src/main/java/com/example/tufantrip/service/admin/SummeryService.java"; Pkg = "com.example.tufantrip.service.admin" }
) | ForEach-Object {
    if (Test-Path $_.Path) {
        $content = Get-Content $_.Path -Raw
        $content = $content -replace '(?m)^package\s+com\.example\.tufantrip\.service;', "package $($_.Pkg);"
        Set-Content $_.Path $content -NoNewline
    }
}

$replacements = [ordered]@{
    "import com.example.tufantrip.service.PermissionService;" = "import access.service.com.aerionsoft.application.PermissionService;"
    "import com.example.tufantrip.service.PermissionGroupService;" = "import access.service.com.aerionsoft.application.PermissionGroupService;"
    "import com.example.tufantrip.service.RoleService;" = "import access.service.com.aerionsoft.application.RoleService;"
    "import com.example.tufantrip.service.ErrorLogService;" = "import errorlog.service.com.aerionsoft.application.ErrorLogService;"
    "import com.example.tufantrip.service.ErrorLogPersistenceService;" = "import errorlog.service.com.aerionsoft.application.ErrorLogPersistenceService;"
    "import com.example.tufantrip.service.ErrorLogPartitionService;" = "import errorlog.service.com.aerionsoft.application.ErrorLogPartitionService;"
    "import com.example.tufantrip.service.ErrorCodeMappingService;" = "import errorlog.service.com.aerionsoft.application.ErrorCodeMappingService;"
    "import com.example.tufantrip.service.MicroserviceErrorHandler;" = "import errorlog.service.com.aerionsoft.application.MicroserviceErrorHandler;"
    "import com.example.tufantrip.service.BreakingNewsService;" = "import breakingnews.service.com.aerionsoft.application.BreakingNewsService;"
    "import com.example.tufantrip.service.BreakingNewsServiceImpl;" = "import breakingnews.service.com.aerionsoft.application.BreakingNewsServiceImpl;"
    "import com.example.tufantrip.service.ActiveUserPresenceService;" = "import user.service.com.aerionsoft.application.ActiveUserPresenceService;"
    "import com.example.tufantrip.service.S3FileService;" = "import com.example.tufantrip.service.common.S3FileService;"
    "import com.example.tufantrip.service.DailyReportService;" = "import report.service.com.aerionsoft.application.DailyReportService;"
    "import com.example.tufantrip.service.SummeryService;" = "import admin.service.com.aerionsoft.application.SummeryService;"
}

Get-ChildItem -Path "src" -Recurse -Filter "*.java" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $original = $content
    foreach ($entry in $replacements.GetEnumerator()) {
        $content = $content.Replace($entry.Key, $entry.Value)
    }
    if ($content -ne $original) { Set-Content $_.FullName $content -NoNewline }
}

Write-Host "Remaining root service files:"
Get-ChildItem "src/main/java/com/example/tufantrip/service" -File -Name
