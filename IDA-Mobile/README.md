# IDA-Mobile

Current scope (single APK, non-root):

- `APK Inspector`:
  - Android SAF picker.
  - Native `mmap` parsing from file descriptor.
  - ZIP central directory scan for entry count, dex count, and manifest presence.
- `Website Inspector`:
  - Direct on-device HTTP request (OkHttp, no backend).
  - Extracts status, content-type, title, script tag count, and SHA-256 sample hash.
- Kotlin + Jetpack Compose UI + C++ core via JNI (`minSdk 30`).

## Build prerequisites

- Android Studio (Hedgehog+ recommended) with:
  - Android SDK Platform 35
  - NDK (26.x or compatible)
  - CMake 3.22.1+
- JDK 17

## Run

```bash
./gradlew :app:assembleDebug
```

If this command succeeds, Kotlin + JNI + native core wiring is healthy.
