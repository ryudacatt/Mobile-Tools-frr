# IDA-Mobile Workbench

<p align="center">
  <img src="docs/assets/project-icon.png" alt="IDA-Mobile icon" width="150" />
</p>

`IDA-Mobile` is an android reverse engineering tool that don't even work nicely trust me ...

## What It Does Today

- Native APK structure inspection through JNI + `mmap`.
- APK debugger workspace
  - package/manifest metadata
  - components and permissions
  - signing SHA-256 and native library listing
- Dalvik assembly view for `classes.dex` with syntax coloring.
- String extraction + cross-reference indexing.
- Embedded Python heuristics with Chaquopy (offline runtime).
- Hex editor workspace:
  - address + hex + ASCII rendering
  - patch bytes at selected offsets
  - export patched APK copy
  - export selected disassembly method to text
- Website inspector (HTTP metadata + payload fingerprinting).
- Radare2 local decompiler bridge (`aflj` + `pdc`) for `classes.dex`.
- Termux bridge panel (detect, launch, run-command intent).

--------------------------------------
` NOTE THIS IS REALLY HEAVY AND LAGGY `
--------------------------------------
## Third-Party Source Policy

The project vendors upstream repositories under `third_party/` and enforces presence during build via `verifyThirdPartySources`.

- `capstone`
- `radare2`
- `lief`
- `ghidra`
- `ogdf`

See [third_party/README.md](third_party/README.md) and [third_party/LOCKFILE.md](third_party/LOCKFILE.md).

## Build

Prerequisites:

- JDK 17
- Android SDK 35
- NDK 26+
- CMake 3.22.1+

Dependency setup:

```powershell
.\scripts\setup_third_party.ps1
```

Build APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Radare2 Binary Setup (For Decompiler Panel)

The decompiler panel is a **radare2 integration**, but it requires ABI-specific `r2` binaries in assets.

Linux/macOS build helper:

```bash
./scripts/build_radare2_android.sh arm64-v8a
```

Windows asset install helper:

```powershell
.\scripts\install_radare2_asset.ps1 -BinaryPath C:\path\to\r2 -Abi arm64-v8a
```

Expected paths:

- `app/src/main/assets/tools/radare2/arm64-v8a/r2`
- `app/src/main/assets/tools/radare2/armeabi-v7a/r2`

## Current Limits

- Full native Ghidra-grade decompiler inside one APK is not currently feasible in this codebase/state bc im dumb and gay
- Full Termux embedding as an internal terminal emulator is not feasible due Android app sandboxing and packaging boundaries so either we go root or don't fk with it for now ... 

## Roadmap

- Multi-dex unified xref index (all DEX files, not only `classes.dex`)
- Native `lib/*.so` disassembly pipeline with Capstone-backed symbol-aware analysis
- CFG generation + touch-first OpenGL graph rendering
- APK binary patch profiles and reproducible patch manifests
- Python plugin API surface with signed plugin loading
- supersu:
  - process attach
  - memory dump
  - no regressions in non-root flows 
  * Imma do these later on in the coming months *