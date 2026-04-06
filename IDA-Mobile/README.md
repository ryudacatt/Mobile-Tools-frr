# IDA-Mobile

Phase 1 foundation for an Android reverse engineering suite:

- Kotlin + Jetpack Compose app (`minSdk 30`, Android 11+ non-root baseline).
- C++ native core built with NDK and CMake.
- JNI bridge verified by calling native code from Compose UI.
- Native folders prepared for later phases (`loader`, `memory`, `disassembler`, `analysis`, `cfg`).

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

If this command succeeds, JNI and native linking are configured correctly.

