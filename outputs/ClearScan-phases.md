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

Status: export/delete/no-monetization foundation implemented; advanced editor tools planned.

- Share/export via Android FileProvider.
- Delete scans from app-private storage.
- Free-policy script checks for ad, billing, and common monetization SDK markers.
- Planned: merge, split, reorder, rotate, compress.
- Planned: signatures, highlight, notes, redaction.
- Planned: password-protected PDF export.
- Planned: tags, folders, favorites, duplicate detection.

Done when:

- Implemented tools work offline.
- Files are generated in app-private storage before explicit export.
- No tool requires payment, account, or cloud.

## Phase 4 - Language And Platform Expansion

Planned.

- Sinhala/Tamil OCR evaluation.
- iOS SwiftUI/VisionKit parity plan.
- Optional self-host export targets such as paperless-ngx/WebDAV.
- Optional local or BYO-key AI classification.

Done when:

- Sinhala/Tamil OCR accuracy is measured on real local test scans.
- iOS architecture is validated against VisionKit/Vision/PDFKit.
- Any AI flow is explicit opt-in and has no hidden upload.
