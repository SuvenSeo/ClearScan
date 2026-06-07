param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)

$ErrorActionPreference = 'Stop'

Write-Host "ClearScan Android environment check"
Write-Host "Project: $ProjectRoot"
Write-Host ""

$java = Get-Command java -ErrorAction SilentlyContinue
if ($java) {
    Write-Host "java: $($java.Source)"
    java -version
} else {
    Write-Host "java: missing"
}

$androidHome = $env:ANDROID_HOME
$androidSdkRoot = $env:ANDROID_SDK_ROOT
Write-Host "ANDROID_HOME: $androidHome"
Write-Host "ANDROID_SDK_ROOT: $androidSdkRoot"

$localProperties = Join-Path $ProjectRoot 'local.properties'
$localSdk = $null
if (Test-Path -LiteralPath $localProperties) {
    $sdkLine = Get-Content -LiteralPath $localProperties |
        Where-Object { $_ -like 'sdk.dir=*' } |
        Select-Object -First 1
    if ($sdkLine) {
        $localSdk = ($sdkLine.Substring('sdk.dir='.Length) -replace '\\:', ':' -replace '\\\\', '\')
    }
}

$candidateSdk = @(
    $localSdk,
    (Join-Path $ProjectRoot 'work\android-sdk'),
    $androidHome,
    $androidSdkRoot,
    "$env:LOCALAPPDATA\Android\Sdk"
) | Where-Object { $_ -and (Test-Path -LiteralPath $_) } | Select-Object -First 1

if ($candidateSdk) {
    Write-Host "Android SDK: $candidateSdk"
} else {
    Write-Host "Android SDK: missing"
}

$gradlew = Join-Path $ProjectRoot 'gradlew.bat'
if (Test-Path -LiteralPath $gradlew) {
    Write-Host "Gradle wrapper: $gradlew"
} else {
    Write-Host "Gradle wrapper: missing; use a local Gradle install to run 'gradle wrapper'."
}
