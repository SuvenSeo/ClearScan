# ClearScan — agent notes

Local-first Android (Kotlin/Compose) document scanner. No backend services, Docker Compose, or Node toolchain. Standard build/smoke commands live in [README.md](README.md); this file covers Cursor Cloud gotchas only.

## Cursor Cloud specific instructions

### Environment assumptions

- **JDK 17 required.** Prefer `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` (do not use the default/21 JVM for Gradle).
- **Android SDK** is expected at `$HOME/android-sdk` (`ANDROID_SDK_ROOT` / `ANDROID_HOME`). Snapshot env already exports these via `~/.bashrc`.
- **`local.properties` is gitignored.** Gradle needs `sdk.dir` pointing at the SDK. If missing after a fresh checkout, recreate with:
  `printf 'sdk.dir=%s\n' "${ANDROID_SDK_ROOT:-$HOME/android-sdk}" > local.properties`
- README’s `work/android-sdk` path is a Windows/local convention; on Cloud Agents use `$HOME/android-sdk`.

### What to run (no services)

This is a single-module Android app — there is nothing to `docker compose up` or `npm`/`pnpm`. Use the Gradle wrapper only.

Canonical Linux smoke (CI’s PowerShell policy scripts are Windows-oriented; Gradle alone is enough here):

```bash
./gradlew :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
```

**No `pwsh` on Cloud** — bash locale-parity script is sufficient; `free-policy *.ps1` needs PowerShell only if you opt into those scripts.

### Known lint / test noise (do not “fix” as env breakage)

`:app:lintDebug` may fail with **pre-existing** code findings (e.g. `NewApi`/`readNBytes`, `LocalContextGetResourceValueCall`, Sinhala `ImpliedQuantity` plurals, `PermissionImpliesUnsupportedChromeOsHardware`). Treat those as product debt, not a broken Cloud environment.

**`LibrarySmokeTest`** may fail when multiple `"Library"` text nodes exist (pre-existing flakiness) even when the Library UI is visible.

Unit tests and `assembleDebug` are the reliable green signals for env health.

### Emulator / device caveats

- Cloud VMs have **no `/dev/kvm`** — start the emulator with **`-accel off`** (software accel only; slow).
- **Google ATD `x86_64` API 30** is light but may ship **GMS too old** for ClearScan — cold start can stick on splash (`Google Play services out of date`). Prefer **`google_apis`** images for interactive UI; **`testDebugUnitTest` + `assembleDebug`** prove env without GMS.
- **Camera + ML Kit Document Scanner** are unreliable here. For end-to-end “hello world”, use the app’s **Import** path instead of live scan.
- **`adb` `ACTION_SEND` of MediaStore URIs** often hits `SecurityException` in `FileImportResolver` — use on-device **SAF Import UI** or instrumentation instead.
- Sample PNGs: `app/src/test/resources/ocr-corpus/` (or create a folder from device storage and import).
- **Vault** flows need a configured **screen lock** on the emulator/device (biometric/device-credential gate).

### Scope reminder

No backend, no compose stack, no Makefile. Lint/test/assemble via `./gradlew`; interactive QA via emulator + Import when camera/ML Kit is unavailable.

### ATD emulator + Play Services

- Google ATD images often ship an **older Play Services** than ML Kit expects. ClearScan may stay on the **splash screen** on ATD (logcat: Play services out of date). Treat that as an emulator-image limitation, not a broken build.
- Prefer JVM unit tests (`:app:testDebugUnitTest`) for core hello-world proof (OCR corpus, PDF tools, vault/crypto, Room). Use a full `google_apis` / Play Store image only when you need past-splash UI or ML Kit scanner.
