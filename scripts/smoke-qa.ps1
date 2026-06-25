param(
    [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$gradlew = Join-Path $Root "gradlew.bat"
if (-not (Test-Path -LiteralPath $gradlew)) {
    throw "Gradle wrapper not found at $gradlew"
}

Write-Host "Running ClearScan QA smoke: lintDebug, testDebugUnitTest, assembleDebug"
& $gradlew :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
if ($LASTEXITCODE -ne 0) {
    throw "QA smoke failed with exit code $LASTEXITCODE"
}

Write-Host "QA smoke passed."
