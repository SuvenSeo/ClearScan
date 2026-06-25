# Release Guide

## Versioning

- `versionCode` and `versionName` live in `app/build.gradle.kts`.
- Bump both for every user-facing release.
- Tag releases as `v{versionName}` on GitHub after pushing to `main`.

## Local debug build

```powershell
.\gradlew.bat :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/`

## Local release build

1. Copy `app/signing.properties.example` to `app/signing.properties`.
2. Point `storeFile` at your release keystore and fill passwords.
3. Run:

```powershell
.\gradlew.bat :app:assembleRelease
```

APK output: `app/build/outputs/apk/release/`

If `signing.properties` is missing, release builds still compile but are unsigned.

## CI

- **Android CI** (`.github/workflows/android-ci.yml`) runs on every push/PR to `main`: free-policy check, lint, unit tests, `assembleDebug`.
- **Android Release** (`.github/workflows/android-release.yml`) is manual (`workflow_dispatch`) and uploads the release APK as a workflow artifact.

## Distribution

- GitHub Releases: attach the signed release APK and update `distribution/version.json` for in-app update checks.
- F-Droid: use metadata under `fastlane/metadata/android/en-US/` and follow [docs/PLAY_STORE_CHECKLIST.md](PLAY_STORE_CHECKLIST.md) for store parity.

## Pre-release checklist

- [ ] `.\gradlew.bat :app:testDebugUnitTest`
- [ ] `.\gradlew.bat :app:assembleDebug` on a device/emulator
- [ ] OCR smoke test on at least one Latin and one Sinhala/Tamil sample
- [ ] Backup export and restore on the same device
- [ ] Privacy policy URL matches [docs/PRIVACY_POLICY.md](PRIVACY_POLICY.md)
