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

$dataExtractionRulesPath = Join-Path $Root "app\src\main\res\xml\data_extraction_rules.xml"
if (-not (Test-Path -LiteralPath $dataExtractionRulesPath)) {
    $findings += [PSCustomObject]@{
        Check = "data-extraction-rules"
        Path = "app/src/main/res/xml/data_extraction_rules.xml"
        Pattern = "missing data_extraction_rules.xml"
    }
} else {
    $dataExtractionRules = Get-Content -LiteralPath $dataExtractionRulesPath -Raw
    if ($dataExtractionRules -notmatch '(?s)<cloud-backup>.*?<exclude\s+domain="database"\s*/>') {
        $findings += [PSCustomObject]@{
            Check = "data-extraction-rules"
            Path = "app/src/main/res/xml/data_extraction_rules.xml"
            Pattern = 'cloud-backup must exclude domain="database"'
        }
    }
    if ($dataExtractionRules -notmatch '(?s)<device-transfer>.*?<exclude\s+domain="database"\s*/>') {
        $findings += [PSCustomObject]@{
            Check = "data-extraction-rules"
            Path = "app/src/main/res/xml/data_extraction_rules.xml"
            Pattern = 'device-transfer must exclude domain="database"'
        }
    }
}

$filePathsPath = Join-Path $Root "app\src\main\res\xml\file_paths.xml"
if (-not (Test-Path -LiteralPath $filePathsPath)) {
    $findings += [PSCustomObject]@{
        Check = "file-paths"
        Path = "app/src/main/res/xml/file_paths.xml"
        Pattern = "missing file_paths.xml"
    }
} else {
    $filePaths = Get-Content -LiteralPath $filePathsPath -Raw
    if ($filePaths -notmatch '(?s)<cache-path\b.*?name="vault_read".*?path="vault-read/"') {
        $findings += [PSCustomObject]@{
            Check = "file-paths"
            Path = "app/src/main/res/xml/file_paths.xml"
            Pattern = 'cache-path must expose vault-read cache (name="vault_read", path="vault-read/")'
        }
    }
}

$backupRestoreManagerPath = Join-Path $Root "app\src\main\kotlin\com\ardeno\clearscan\backup\BackupRestoreManager.kt"
if (-not (Test-Path -LiteralPath $backupRestoreManagerPath)) {
    $findings += [PSCustomObject]@{
        Check = "zip-slip"
        Path = "app/src/main/kotlin/com/ardeno/clearscan/backup/BackupRestoreManager.kt"
        Pattern = "missing BackupRestoreManager.kt"
    }
} else {
    $backupRestoreManager = Get-Content -LiteralPath $backupRestoreManagerPath -Raw
    $hasSafeZipEntryPath = $backupRestoreManager -match 'safeZipEntryPath'
    $hasCanonicalPathCheck = $backupRestoreManager -match 'canonicalFile'
    if (-not $hasSafeZipEntryPath -and -not $hasCanonicalPathCheck) {
        $findings += [PSCustomObject]@{
            Check = "zip-slip"
            Path = "app/src/main/kotlin/com/ardeno/clearscan/backup/BackupRestoreManager.kt"
            Pattern = "must define safeZipEntryPath or canonical path check for zip extraction"
        }
    }
}

if ($findings.Count -gt 0) {
    Write-Host "Privacy release gate failed:"
    $findings | Format-Table -AutoSize
    exit 1
}

Write-Host "Privacy release gate passed: no ad/billing SDK markers, allowBackup=false, encrypted storage present, data extraction rules exclude database, vault-read cache path configured, zip-slip guards present."
