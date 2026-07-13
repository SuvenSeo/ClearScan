# ClearScan

ClearScan is an Android-first, local-first document scanner planned as a free CamScanner-style alternative without ads, subscriptions, watermarks, accounts, forced cloud storage, or feature paywalls.

## Phase Status

- Phase 0 product and technical direction: complete.
- Phase 1 scanner foundation: complete.
- Phase 2 OCR and searchable PDFs: implemented foundation (real scan accuracy QA still needed).
- Phase 3 free PDF tools: feature-complete pending QA (annotation, folders, tags, batch, duplicates implemented).
- Phase 4 language and platform expansion: in progress (Sinhala/Tamil OCR benchmarking, self-host export, ID scan mode).
- Phase 5 security, backup, and privacy dashboard: implemented foundation.
- Phase 6 Tier 3 local intelligence: implemented foundation (page-turn capture, receipt extraction, auto-tagging, image enhancement).

See [docs/PHASES.md](docs/PHASES.md) for done-when criteria per phase.

## Current Release

**v0.2.3** — Full remediation pass: metadata encryption, biometric-bound vault keys, ViewModel split, si/ta localization foundation, expanded OCR corpus (27 entries per language), CamScanner competitive research, Robolectric unit-test harness.

Download: [GitHub Releases](https://github.com/SuvenSeo/ClearScan/releases/latest)

## Current App Scope

- Native Kotlin + Jetpack Compose app shell.
- ML Kit Document Scanner launch path.
- PDF/JPEG result capture from the scanner intent.
- Private app-storage persistence for returned scan files.
- Local JSON index for scanned-document metadata.
- Local OCR over saved page images.
- OCR text search across saved documents.
- Searchable PDF export generated locally from page images plus OCR text.
- PDF editor actions: merge all scans, split into page PDFs, rotate pages, add typed signature, redact a header band, and create password-protected PDFs.
- Biometric/device-credential vault gate with Android Keystore crypto health check and optional biometric-bound keys.
- Encrypted metadata at rest for `index.json`, folders index, and Room document payloads.
- Sinhala/Tamil OCR benchmark harness for CER/WER scoring plus 27-entry labeled corpus per language (`tools/ocr-corpus/`).
- Compose UI string resources for English, Sinhala (si), and Tamil (ta) on core screens.
- Encrypted document storage at rest, privacy dashboard, and encrypted local backup/restore.
- Self-host export (WebDAV / paperless-ngx), ID scan mode with redaction suggestions.
- Page-turn capture, receipt field extraction, offline auto-tagging, and scan quality enhancement.
- Android share-sheet export through FileProvider.
- Delete saved scans from app-private storage.
- Offline-first UI with document list, scan/import actions, OCR status, and free-feature surface.

## Why ClearScan

ClearScan is not trying to beat CamScanner by copying its brand or UI. It competes on trust and freedom:

- No ads in the scanner core.
- No subscriptions for core scanning, OCR, PDF tools, folders, search, signatures, redaction, password export, or export.
- No watermark on exported files.
- No forced account or forced cloud upload.
- Local-first document storage by default.
- Future self-host/BYO-key AI instead of paid cloud lock-in.

See [docs/COMPETITIVE_POSITIONING.md](docs/COMPETITIVE_POSITIONING.md) for the detailed CamScanner comparison.

## Local Build

This workspace has a local Android SDK installed under `work/android-sdk`, with `local.properties` pointing Gradle to it.

Run:

```powershell
.\scripts\smoke-qa.ps1
```

Or run Gradle directly:

```powershell
.\gradlew.bat :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
```

Privacy release gate (pre-release):

```powershell
.\scripts\privacy-release-gate.ps1
```

## Final Debug Build

The verified debug APK is published on GitHub Releases:

- Latest build: https://github.com/SuvenSeo/ClearScan/releases/tag/v0.2.2
- APK: `ClearScan-v0.2.2-debug.apk` ([direct download](https://github.com/SuvenSeo/ClearScan/releases/download/v0.2.2/ClearScan-v0.2.2-debug.apk))

In-app updates (opt-in): see [docs/APP_UPDATES.md](docs/APP_UPDATES.md). The app reads `distribution/version.json` on `main` and downloads newer APKs from GitHub Releases.

Free-policy guard:

```powershell
.\scripts\check-free-policy.ps1
```

## Product Rules

- No CamScanner branding, copied UI, copied assets, or misleading compatibility claims.
- No ad SDKs or hidden analytics in the scanner core.
- No billing/subscription SDKs in the scanner core.
- No document upload unless the user explicitly chooses an export/sync action.
- Keep expensive cloud AI outside the free core unless implemented through local models, self-hosting, donations, or user-provided API keys.
- Keep password-protected PDFs and vault behavior local-first; do not add forced cloud recovery.
