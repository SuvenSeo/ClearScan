param(
    [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

function Get-StringResourceKeys {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        Write-Error "Strings file not found: $Path"
    }

    $text = Get-Content -LiteralPath $Path -Raw
    $matches = [regex]::Matches($text, '<(?:string|plurals)\s+name="([^"]+)"')
    $keys = New-Object 'System.Collections.Generic.HashSet[string]'
    foreach ($match in $matches) {
        [void]$keys.Add($match.Groups[1].Value)
    }
    return $keys
}

$basePath = Join-Path $Root "app\src\main\res\values\strings.xml"
$siPath = Join-Path $Root "app\src\main\res\values-si\strings.xml"
$taPath = Join-Path $Root "app\src\main\res\values-ta\strings.xml"

$baseKeys = Get-StringResourceKeys -Path $basePath
$siKeys = Get-StringResourceKeys -Path $siPath
$taKeys = Get-StringResourceKeys -Path $taPath

$failed = $false

$siMissing = @($baseKeys | Where-Object { -not $siKeys.Contains($_) } | Sort-Object)
if ($siMissing.Count -gt 0) {
    Write-Host "values-si/strings.xml is missing keys present in values/strings.xml:"
    foreach ($key in $siMissing) {
        Write-Host "  $key"
    }
    $failed = $true
}

$taMissing = @($baseKeys | Where-Object { -not $taKeys.Contains($_) } | Sort-Object)
if ($taMissing.Count -gt 0) {
    Write-Host "values-ta/strings.xml is missing keys present in values/strings.xml:"
    foreach ($key in $taMissing) {
        Write-Host "  $key"
    }
    $failed = $true
}

if ($failed) {
    exit 1
}

Write-Host "Locale string parity check passed (si/ta cover all values/ keys)."
