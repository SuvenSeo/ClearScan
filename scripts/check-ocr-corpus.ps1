param(
    [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$corpusDirs = @(
    (Join-Path $Root "app/src/test/resources/ocr-corpus"),
    (Join-Path $Root "tools/ocr-corpus")
)

$jsonCount = 0
foreach ($dir in $corpusDirs) {
    if (-not (Test-Path -LiteralPath $dir)) {
        Write-Warning "OCR corpus directory missing: $dir"
        continue
    }

    $jsonCount += @(Get-ChildItem -LiteralPath $dir -Filter "*.json" -File).Count
}

Write-Host "OCR corpus JSON files: $jsonCount (across $($corpusDirs.Count) directories)"

if ($jsonCount -lt 10) {
    Write-Warning "OCR corpus has fewer than 10 JSON files ($jsonCount). Add more entries to improve OCR regression coverage."
}

$tessdataDir = Join-Path $Root "app/src/main/assets/tessdata"
$requiredTessdata = @("sin.traineddata", "tam.traineddata")
$missingTessdata = @()

foreach ($file in $requiredTessdata) {
    $path = Join-Path $tessdataDir $file
    if (-not (Test-Path -LiteralPath $path)) {
        $missingTessdata += $path
    }
}

if ($missingTessdata.Count -gt 0) {
    Write-Host "OCR tessdata check failed. Missing required files:"
    $missingTessdata | ForEach-Object { Write-Host "  $_" }
    exit 1
}

Write-Host "OCR tessdata check passed: sin.traineddata and tam.traineddata present."
