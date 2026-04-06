param(
    [Parameter(Mandatory = $true)][string]$BinaryPath,
    [string]$Abi = "arm64-v8a",
    [string]$Root = (Resolve-Path "$PSScriptRoot\..").Path
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $BinaryPath)) {
    throw "Binary not found: $BinaryPath"
}

$targetDir = Join-Path $Root "app/src/main/assets/tools/radare2/$Abi"
New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

$target = Join-Path $targetDir "r2"
Copy-Item -LiteralPath $BinaryPath -Destination $target -Force

Write-Host "Installed radare2 asset:"
Write-Host $target
Write-Host "On device, app will copy this binary to internal storage and mark it executable."
