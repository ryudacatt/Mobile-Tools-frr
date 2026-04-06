param(
    [string]$Root = (Resolve-Path "$PSScriptRoot\..").Path
)

$ErrorActionPreference = "Stop"

function Clone-IfMissing {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][string]$Target
    )

    if (Test-Path $Target) {
        Write-Host "Skipping existing: $Target"
        return
    }

    Write-Host "Cloning $Url -> $Target"
    git clone $Url $Target
}

$thirdParty = Join-Path $Root "third_party"
New-Item -ItemType Directory -Force -Path $thirdParty | Out-Null

$gitRoot = (& git -C $Root rev-parse --show-toplevel).Trim()
$submoduleFile = Join-Path $gitRoot ".gitmodules"

if (Test-Path $submoduleFile) {
    Write-Host "Submodule mode detected. Syncing and updating third_party submodules..."
    git -C $gitRoot submodule sync --recursive
    git -C $gitRoot submodule update --init --recursive `
        IDA-Mobile/third_party/capstone `
        IDA-Mobile/third_party/radare2 `
        IDA-Mobile/third_party/lief `
        IDA-Mobile/third_party/ghidra `
        IDA-Mobile/third_party/ogdf
} else {
    Write-Host "No .gitmodules found. Falling back to direct clone mode..." 
    Clone-IfMissing -Url "https://github.com/capstone-engine/capstone.git" -Target (Join-Path $thirdParty "capstone")
    Clone-IfMissing -Url "https://github.com/radareorg/radare2.git" -Target (Join-Path $thirdParty "radare2")
    Clone-IfMissing -Url "https://github.com/lief-project/LIEF.git" -Target (Join-Path $thirdParty "lief")
    Clone-IfMissing -Url "https://github.com/NationalSecurityAgency/ghidra.git" -Target (Join-Path $thirdParty "ghidra")
    Clone-IfMissing -Url "https://github.com/ogdf/ogdf.git" -Target (Join-Path $thirdParty "ogdf")
}

Write-Host "Dependency setup complete."