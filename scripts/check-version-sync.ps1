param(
    [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$gradlePath = Join-Path $Root "app\build.gradle.kts"
$versionJsonPath = Join-Path $Root "distribution\version.json"

if (-not (Test-Path -LiteralPath $gradlePath)) {
    Write-Error "Gradle file not found: app/build.gradle.kts"
}

if (-not (Test-Path -LiteralPath $versionJsonPath)) {
    Write-Error "Version file not found: distribution/version.json"
}

$gradleText = Get-Content -LiteralPath $gradlePath -Raw

if ($gradleText -notmatch 'versionCode\s*=\s*(\d+)') {
    Write-Error "Could not parse versionCode from app/build.gradle.kts"
}
$gradleVersionCode = [int]$Matches[1]

if ($gradleText -notmatch 'versionName\s*=\s*"([^"]+)"') {
    Write-Error "Could not parse versionName from app/build.gradle.kts"
}
$gradleVersionName = $Matches[1]

$versionJson = Get-Content -LiteralPath $versionJsonPath -Raw | ConvertFrom-Json
$jsonVersionCode = [int]$versionJson.versionCode
$jsonVersionName = [string]$versionJson.versionName

$mismatches = @()

if ($gradleVersionCode -ne $jsonVersionCode) {
    $mismatches += "versionCode: Gradle=$gradleVersionCode, version.json=$jsonVersionCode"
}

if ($gradleVersionName -ne $jsonVersionName) {
    $mismatches += "versionName: Gradle=`"$gradleVersionName`", version.json=`"$jsonVersionName`""
}

if ($mismatches.Count -gt 0) {
    Write-Host "Version sync check failed:"
    foreach ($mismatch in $mismatches) {
        Write-Host "  $mismatch"
    }
    exit 1
}

Write-Host "Version sync check passed:"
Write-Host "  versionCode: $gradleVersionCode"
Write-Host "  versionName: $gradleVersionName"
