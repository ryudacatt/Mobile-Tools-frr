# IDA-Mobile Workbench

`IDA-Mobile Workbench` is a standalone Android reverse engineering toolkit focused on **real, local-first analysis** of APKs and websites on Android 11+ (non-root).

The current build ships as one APK and includes:

- Native APK structure inspection (`mmap` + JNI + C++).
- APK debugger workspace (manifest/package/components/permissions/signing/native libs).
- Assembly viewer for `classes.dex` (real Dalvik bytecode decoding with syntax coloring).
- Fast in-app find/search for debugger and assembly data.
- Website inspector with local HTTP parsing and content fingerprinting.

No backend service is required for these features.

## Why This Project Exists

Most mobile reverse-engineering workflows still depend on desktop tools, shell scripts, and mixed environments.  
This project pushes that workflow directly onto-device with a mobile-native UX while keeping the architecture strict and testable:

- Kotlin + Compose frontend.
- Native C++ core for binary-heavy operations.
- JNI boundary for controlled interoperability.
- One packaged APK for direct deployment.

## Current Feature Set

### 1. APK Overview (Native)

- Reads selected APK from SAF.
- Uses native `mmap` to parse ZIP headers and central directory.
- Returns:
  - File size
  - ZIP entry count
  - DEX file count
  - `AndroidManifest.xml` presence

### 2. APK Debugger Workspace

- Builds static debug context from archive metadata via `PackageManager`.
- Extracts:
  - Package name, version, SDK levels
  - Debuggable flag
  - Permissions
  - Activities/services/receivers/providers
  - Native `lib/*.so` entries
  - Signing certificate SHA-256 digests
- Includes on-device filtering (`find`) for all major sections.

### 3. Assembly Viewer

- Loads `classes.dex` from APK zip.
- Decodes Dalvik instructions with `dexlib2`.
- Displays methods + instruction stream in-app.
- Includes syntax color engine:
  - Address
  - Mnemonic
  - Registers
  - Literals
  - Comments
- Includes method/class query filtering and method selection.

### 4. Website Inspector

- On-device HTTP request (OkHttp).
- Extracts:
  - Final normalized URL
  - HTTP status
  - Content type
  - Sampled payload size
  - HTML title
  - Script tag count
  - SHA-256 fingerprint of sampled payload

## Architecture

```text
app/
  ui/                 Compose screens and tool panels
  viewmodels/         Orchestration and state
  fileloader/         APK materialization + inspector/debugger repos
  disassembly/        DEX decode + syntax highlighter
  features/websites/  Website analysis repository
native/
  loader/             mmap APK parser
  jni/                NativeBridge bindings
  core/               Core version + diagnostics
```

### Data Flow (APK path)

1. User picks APK via SAF.
2. APK is materialized to app cache.
3. Three analyzers run:
   - Native ZIP/DEX summary
   - Package/debugger metadata extraction
   - DEX assembly decoding
4. UI presents each tool mode from one synchronized workspace state.

## Build Instructions

## Prerequisites

- Android Studio (Hedgehog+ recommended).
- Android SDK Platform 35.
- NDK 26.x (or compatible).
- CMake 3.22.1+.
- JDK 17.

## Build Debug APK

```bash
./gradlew :app:assembleDebug
```

Output APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install on Device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Operational Notes

- Works without root.
- All current tooling runs locally on-device.
- Assembly decoding is intentionally bounded (method/instruction limits) to keep UI responsive on mobile hardware.
- APK workspace artifacts are stored in app cache and can be cleared by app data reset.

## Limitations (Current)

The following are intentionally not claimed as complete yet:

- Full live process debugger with breakpoints/stepping on arbitrary targets.
- Full decompiler equivalent to desktop IDA/Ghidra output.
- Multi-dex full graph/xref explorer across all dex files.

These are advanced phases and require additional hardened modules.

## Future Goals

- Multi-dex decode and unified cross-reference index.
- Native `lib/*.so` disassembly path with Capstone integration.
- CFG graph rendering with touch-first navigation.
- String/xref browser with reverse navigation.
- Plugin SDK (Python/Chaquopy) with safe API surface.
- Optional root mode module for process attach/memory dump, without breaking non-root flows.
- Export workflows for report bundles and reproducible analysis snapshots.

## Quality Targets

- No fake features.
- No silent fallbacks.
- Clear error surfacing per tool.
- Deterministic parsing and stable output formats.
- Mobile-safe memory and rendering boundaries.

---

If you are extending the project, keep every addition compile-ready, testable, and explicitly scoped.

