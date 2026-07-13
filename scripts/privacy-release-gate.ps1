param(
    [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$blockedPatterns = @(
    "play-services-ads",
    "firebase-ads",
    "admob",
    "applovin",
    "ironsource",
    "unity-ads",
    "billingclient",
    "billing-ktx",
    "com.android.billingclient"
)

$excludedDirectoryNames = @(".gradle", ".kotlin", "build", "work", "outputs")
$dependencyFiles = Get-ChildItem -LiteralPath $Root -Recurse -File |
    Where-Object {
        $file = $_
        foreach ($directoryName in $excludedDirectoryNames) {
            if ($file.FullName -like "*\$directoryName\*") {
                return $false
            }
        }

        return (
            $file.Name.EndsWith(".gradle.kts") -or
            $file.Name.EndsWith(".toml")
        )
    }

$findings = @()
foreach ($file in $dependencyFiles) {
    $text = Get-Content -LiteralPath $file.FullName -Raw
    foreach ($pattern in $blockedPatterns) {
        if ($text -match [Regex]::Escape($pattern)) {
            $findings += [PSCustomObject]@{
                Check = "ad-or-billing-sdk"
                Path = $file.FullName.Substring($Root.Length + 1)
                Pattern = $pattern
            }
        }
    }
}

$manifestPath = Join-Path $Root "app\src\main\AndroidManifest.xml"
if (-not (Test-Path -LiteralPath $manifestPath)) {
    $findings += [PSCustomObject]@{
        Check = "manifest"
        Path = "app/src/main/AndroidManifest.xml"
        Pattern = "missing manifest"
    }
} else {
    $manifest = Get-Content -LiteralPath $manifestPath -Raw
    if ($manifest -notmatch 'android:allowBackup\s*=\s*"false"') {
        $findings += [PSCustomObject]@{
            Check = "allowBackup"
            Path = "app/src/main/AndroidManifest.xml"
            Pattern = 'android:allowBackup must be "false"'
        }
    }
}

$encryptedStorePath = Join-Path $Root "app\src\main\kotlin\com\ardeno\clearscan\vault\EncryptedFileStore.kt"
if (-not (Test-Path -LiteralPath $encryptedStorePath)) {
    $findings += [PSCustomObject]@{
        Check = "encrypted-storage"
        Path = "app/src/main/kotlin/com/ardeno/clearscan/vault/EncryptedFileStore.kt"
        Pattern = "missing EncryptedFileStore"
    }
} else {
    $encryptedStore = Get-Content -LiteralPath $encryptedStorePath -Raw
    if ($encryptedStore -notmatch '\.enc|ENCRYPTED_SUFFIX|encryptPlaintextFile') {
        $findings += [PSCustomObject]@{
            Check = "encrypted-storage"
            Path = "app/src/main/kotlin/com/ardeno/clearscan/vault/EncryptedFileStore.kt"
            Pattern = "encrypted document path API not found"
        }
    }
}

$backupRestorePath = Join-Path $Root "app\src\main\kotlin\com\ardeno\clearscan\backup\BackupRestoreManager.kt"
if (-not (Test-Path -LiteralPath $backupRestorePath)) {
    $findings += [PSCustomObject]@{
        Check = "backup-zip-safety"
        Path = "app/src/main/kotlin/com/ardeno/clearscan/backup/BackupRestoreManager.kt"
        Pattern = "missing BackupRestoreManager"
    }
} else {
    $backupRestore = Get-Content -LiteralPath $backupRestorePath -Raw
    if ($backupRestore -notmatch 'safeZipEntryPath') {
        $findings += [PSCustomObject]@{
            Check = "backup-zip-safety"
            Path = "app/src/main/kotlin/com/ardeno/clearscan/backup/BackupRestoreManager.kt"
            Pattern = "safeZipEntryPath not found (zip path traversal guard missing)"
        }
    }
}

if ($findings.Count -gt 0) {
    Write-Host "Privacy release gate failed:"
    $findings | Format-Table -AutoSize
    exit 1
}

Write-Host "Privacy release gate passed: no ad/billing SDK markers, allowBackup=false, encrypted storage present, backup zip path validation present."
