# ClearScan Phase Plan

## Phase 0 - Product And Technical Direction

Status: complete.

- Android-first native app.
- Local-first document processing.
- ML Kit Document Scanner for capture.
- ML Kit OCR first, then Sinhala/Tamil OCR research through Tesseract or PaddleOCR.
- No ads, watermark, forced account, or forced cloud upload.

## Phase 1 - Scanner Foundation

Status: complete.

- Compose app shell.
- Scan/import launch through ML Kit Document Scanner.
- Persist returned PDF/JPEG files into app-private storage.
- Maintain local document metadata index.
- Show scan library and free core tool surface.

Done when:

- `:app:assembleDebug` passes with Android SDK installed.
- Real device scan creates a local document entry.
- Airplane-mode smoke confirms the library opens without network.

## Phase 2 - OCR And Searchable PDFs

Status: implemented foundation, needs real scan accuracy QA.

- OCR queue managed by the app ViewModel for the current local session.
- Extract page text from saved scans.
- Store OCR text in local index.
- Build searchable PDF output with a low-alpha text layer over saved page images.
- Add search UI across document titles and OCR text.

Done when:

- `:app:testDebugUnitTest`, `:app:lintDebug`, and `:app:assembleDebug` pass.
- Device smoke confirms the updated library/search UI opens.
- A real document scan confirms OCR text and searchable PDF export quality.

## Phase 3 - Free PDF Tools

Status: Tier 1 PDF editor tools complete; annotation and organization features in progress.

- Share/export via Android FileProvider.
- Delete scans from app-private storage.
- Free-policy script checks for ad, billing, and common monetization SDK markers.
- Merge all scans in the local library into a new PDF.
- Split a scan into single-page PDFs.
- Rotate pages into a new PDF.
- Add a typed signature to a new PDF copy.
- Apply a header-band redaction to a new PDF copy.
- Create a password-protected PDF using PDFBox-Android.
- Freehand signature drawing, highlights, sticky notes, and arbitrary-area redaction through the page annotator.
- User-facing folders (create, rename, move documents).
- Tags on documents (add/remove from detail sheet).
- Favorites/starred documents.
- Batch multi-select merge, bulk export, and bulk delete.
- Duplicate detection using perceptual hashes (dHash) on saved page images.
- Reorder pages within a document (new copy).
- Delete individual pages from a document (new copy).
- Compress PDF on-device with high / balanced / smallest-size presets.
- Import existing PDFs and images via Android Storage Access Framework file picker.

Done when:

- Implemented tools work offline.
- Files are generated in app-private storage before explicit export.
- No tool requires payment, account, or cloud.
- Library supports folders, tags, favorites, batch actions, and duplicate badges.

## Phase 4 - Language And Platform Expansion

Status: in progress — self-host export and ID scan mode implemented; Sinhala/Tamil OCR benchmarking continues.

- Sinhala/Tamil OCR CER/WER benchmark scoring harness.
- Planned: real Sinhala/Tamil scan corpus and OCR engine comparison.
- iOS SwiftUI/VisionKit parity plan.
- **Self-host export (opt-in):** WebDAV upload for Nextcloud/generic endpoints; paperless-ngx REST upload via API token. Credentials stored in EncryptedSharedPreferences; explicit per-document Upload action only.
- **ID / passport scan mode:** ML Kit base scanner preset (2-page limit), `id-card` tagging, no export watermark, OCR-driven sensitive-field redaction suggestions with one-tap ID redaction tool.
- Optional local or BYO-key AI classification.

Done when:

- Sinhala/Tamil OCR accuracy is measured on real local test scans.
- iOS architecture is validated against VisionKit/Vision/PDFKit.
- Any AI flow is explicit opt-in and has no hidden upload.
- Self-host upload succeeds against a user-configured WebDAV or paperless-ngx test instance.
- ID scans surface redaction suggestions after OCR and produce a redacted local copy on demand.

## Phase 5 - Security, Backup, And Privacy Dashboard

Status: implemented foundation.

- Encrypt document blobs at rest with Android Keystore AES/GCM (`EncryptedFileStore`, `.enc` files).
- Migrate legacy plaintext scans on first load after upgrade.
- Explicit encrypted local backup/restore via SAF (`.csbak`, Keystore-bound to device).
- Privacy dashboard: offline policy, storage path, export audit log, ad SDK scan, encryption health.
- Biometric vault lock composes with encryption at rest.

Done when:

- `:app:assembleDebug` passes.
- New scans are stored as encrypted blobs; OCR/PDF tools read from decrypted cache.
- Export backup and restore round-trip on the same device.
- Privacy dashboard reflects storage path and export audit entries after share.

## Phase 6 - Tier 3 Local Intelligence (Selective)

Status: implemented foundation.

- Auto page-turn capture via CameraX frame differencing (`PageTurnDetector`) when enabled in Settings.
- Local receipt field extraction (merchant, date, amount) from OCR text using regex heuristics.
- Offline document auto-tagging (`receipt`, `invoice`, `certificate`, `id`) from OCR keyword signals.
- Scan quality post-processing (`ImageEnhancer`) for shadow lift and glare compression — no cloud, no heavy ML deps.
- Skipped by design: CS AI chat, study tutor, cloud image detector.

Done when:

- `:app:assembleDebug` passes.
- Settings toggles control page-turn capture and image enhancement.
- OCR pipeline merges suggested tags and receipt fields into the local document index.
