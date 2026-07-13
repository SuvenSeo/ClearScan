param(
    [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$corpusDir = Join-Path $Root "app/src/test/resources/ocr-corpus"
$authoringDir = Join-Path $Root "tools/ocr-corpus"
$minPerLanguage = 25

if (-not (Test-Path -LiteralPath $corpusDir)) {
    Write-Warning "OCR corpus directory missing: $corpusDir"
    exit 1
}

$entryFiles = @(Get-ChildItem -LiteralPath $corpusDir -Filter "*.json" -File | Where-Object { $_.Name -ne "index.json" })
$jsonCount = $entryFiles.Count

Write-Host "OCR corpus entry JSON files: $jsonCount (in $corpusDir)"

$sinhalaCount = 0
$tamilCount = 0

foreach ($file in $entryFiles) {
    $content = Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json
    switch ($content.language) {
        "sinhala" { $sinhalaCount++ }
        "tamil" { $tamilCount++ }
        default { Write-Warning "Unknown language in $($file.Name): $($content.language)" }
    }
}

Write-Host "Sinhala entries: $sinhalaCount"
Write-Host "Tamil entries: $tamilCount"

if ($sinhalaCount -lt $minPerLanguage) {
    Write-Warning "OCR corpus has fewer than $minPerLanguage Sinhala entries ($sinhalaCount)."
}

if ($tamilCount -lt $minPerLanguage) {
    Write-Warning "OCR corpus has fewer than $minPerLanguage Tamil entries ($tamilCount)."
}

if (-not (Test-Path -LiteralPath $authoringDir)) {
    Write-Warning "OCR corpus authoring directory missing: $authoringDir"
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
