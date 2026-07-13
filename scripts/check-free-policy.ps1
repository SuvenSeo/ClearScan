param(
    [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

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

$excludedFileSuffixes = @(
    "check-free-policy.ps1",
    "privacy-release-gate.ps1",
    "PrivacyStatusProvider.kt"
)
$excludedDirectoryNames = @(".gradle", ".kotlin", "build", "work", "outputs")
$scanFiles = Get-ChildItem -LiteralPath $Root -Recurse -File |
    Where-Object {
        $file = $_
        foreach ($excluded in $excludedFileSuffixes) {
            if ($file.Name -eq $excluded) {
                return $false
            }
        }

        foreach ($directoryName in $excludedDirectoryNames) {
            if ($file.FullName -like "*\$directoryName\*") {
                return $false
            }
        }

        return (
            $file.Name.EndsWith(".gradle.kts") -or
            $file.Name.EndsWith(".toml") -or
            $file.Name.EndsWith(".kt") -or
            $file.Name.EndsWith(".xml")
        )
    }

$findings = @()
foreach ($file in $scanFiles) {
    $text = Get-Content -LiteralPath $file.FullName -Raw
    foreach ($pattern in $blockedPatterns) {
        if ($text -match [Regex]::Escape($pattern)) {
            $findings += [PSCustomObject]@{
                Path = $file.FullName.Substring($Root.Length + 1)
                Pattern = $pattern
            }
        }
    }
}

if ($findings.Count -gt 0) {
    Write-Host "Free policy check failed. Remove ad, billing, or monetization SDK markers:"
    $findings | Format-Table -AutoSize
    exit 1
}

Write-Host "Free policy check passed: no ad, billing, or monetization SDK markers found."
