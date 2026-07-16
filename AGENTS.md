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

### Known lint noise (do not “fix” as env breakage)

`:app:lintDebug` may fail with **pre-existing** code findings (e.g. `NewApi`/`readNBytes`, `LocalContextGetResourceValueCall`, Sinhala `ImpliedQuantity` plurals, `PermissionImpliesUnsupportedChromeOsHardware`). Treat those as product debt, not a broken Cloud environment. Unit tests and `assembleDebug` are the reliable green signals for env health.

### Emulator / device caveats

- Cloud VMs typically have **no `/dev/kvm`**. Start the emulator with **`-accel off`** (software accel only — slow).
- Prefer a light AVD: **Google ATD `x86_64` API 30** over heavy Google APIs images.
- **Camera + ML Kit Document Scanner** are unreliable/hard in this environment. For end-to-end “hello world”, use the app’s **Import** path instead of live scan.
- Sample PNGs: `app/src/test/resources/ocr-corpus/` (or create a folder from device storage and import).
- **Vault** flows need a configured **screen lock** on the emulator/device (biometric/device-credential gate).

### Scope reminder

No backend, no compose stack, no Makefile. Lint/test/assemble via `./gradlew`; interactive QA via emulator + Import when camera/ML Kit is unavailable.
