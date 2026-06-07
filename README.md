# ClearScan

ClearScan is an Android-first, local-first document scanner planned as a free CamScanner-style alternative without ads, subscriptions, watermarks, accounts, forced cloud storage, or feature paywalls.

## Phase Status

- Phase 0 research: complete.
- Phase 1 Android foundation: complete.
- Phase 2 OCR/search/searchable PDF foundation: implemented.
- Phase 3 free export/delete/no-monetization guard: implemented.
- Phase 3 advanced PDF editing, app lock, backup, tags: planned.
- Phase 4 Sinhala/Tamil OCR and iOS parity: planned.

## Current App Scope

- Native Kotlin + Jetpack Compose app shell.
- ML Kit Document Scanner launch path.
- PDF/JPEG result capture from the scanner intent.
- Private app-storage persistence for returned scan files.
- Local JSON index for scanned-document metadata.
- Local OCR over saved page images.
- OCR text search across saved documents.
- Searchable PDF export generated locally from page images plus OCR text.
- Android share-sheet export through FileProvider.
- Delete saved scans from app-private storage.
- Offline-first UI with document list, scan/import actions, OCR status, and free-feature surface.

## Why ClearScan

ClearScan is not trying to beat CamScanner by copying its brand or UI. It competes on trust and freedom:

- No ads in the scanner core.
- No subscriptions for core scanning, OCR, PDF tools, folders, search, signatures, or export.
- No watermark on exported files.
- No forced account or forced cloud upload.
- Local-first document storage by default.
- Future self-host/BYO-key AI instead of paid cloud lock-in.

See [docs/COMPETITIVE_POSITIONING.md](docs/COMPETITIVE_POSITIONING.md) for the detailed CamScanner comparison.

## Local Build

This workspace has a local Android SDK installed under `work/android-sdk`, with `local.properties` pointing Gradle to it.

Run:

```powershell
.\gradlew.bat :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
```

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
