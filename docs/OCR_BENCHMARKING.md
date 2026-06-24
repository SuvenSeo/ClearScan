# Sinhala/Tamil OCR Benchmarking

ClearScan includes a CER/WER scoring harness and on-device Sinhala/Tamil OCR via Tesseract.

## Engine Selection (Tier 2)

| Engine | Role | Rationale |
|--------|------|-----------|
| **ML Kit Latin** | Default for Latin / English | Fast, small, already integrated |
| **Tesseract 5 (LSTM)** | Sinhala (`sin`) and Tamil (`tam`) | Official tessdata, fully offline, mature Android wrapper |
| PaddleOCR | Not chosen | Larger native/runtime footprint; heavier integration for marginal gain without a local corpus |

**Privacy:** All OCR runs on-device. Traineddata ships in app assets and is copied to private storage on first use. No hidden cloud upload.

## What Is Implemented

- `OcrLanguage` picker in Settings (default) and per-document in the detail sheet.
- `OcrEngine` routes Latin → ML Kit, Sinhala/Tamil → Tesseract.
- `TessDataInstaller` copies bundled `sin.traineddata` / `tam.traineddata` into app-private storage.
- OCR queue and searchable PDF generation use the selected language.
- `OcrBenchmark` CER/WER harness with unit tests (`OcrBenchmarkTest`).
- `OcrBenchmarkRunner` synthetic print benchmark (rendered text → OCR → CER/WER).
- In-app **Run self-check** (Settings → Developer) runs the synthetic engine benchmark on-device.

## Measured Results

### Harness unit tests (JVM)

`./gradlew :app:testDebugUnitTest --tests "com.ardeno.clearscan.ocr.OcrBenchmarkTest"` — passes; validates CER/WER math only.

### Synthetic on-device benchmark (Tesseract 5 LSTM)

Run from the app: **Settings → Developer → Run self-check**.

Samples: rendered PNG text (1280×360, 72pt) for:

- Sinhala: `සිංහල ලිපිය`
- Tamil: `தமிழ் ஆவணம்`

**Note:** Synthetic print samples use the system default font, which may not include full Sinhala/Tamil glyphs. CER/WER on these samples reflects engine + font coverage, not real document scan quality. Treat numbers as a smoke test until the scan corpus below exists.

| Language | Sample | CER | WER | Notes |
|----------|--------|-----|-----|-------|
| Sinhala | sinhala-synthetic-print | Run self-check in app | — | Requires device/emulator |
| Tamil | tamil-synthetic-print | Run self-check in app | — | Requires device/emulator |

### Real scan corpus (still needed)

Do not claim parity with CamScanner or production-grade Sinhala/Tamil accuracy until this corpus is labeled and scored:

- At least 25 Sinhala document scans.
- At least 25 Tamil document scans.
- Ground-truth text manually typed from each scan.
- Separate results for printed text, handwritten text, receipts, forms, and low-light scans.

## Benchmark Rule

Do not claim Sinhala/Tamil OCR is production-ready for all document types until a chosen engine is evaluated against the real scan corpus and this file is updated with measured CER/WER per category.

## Files

| Area | Path |
|------|------|
| Language model | `app/.../ocr/OcrLanguage.kt` |
| Engine facade | `app/.../ocr/OcrEngine.kt` |
| Tesseract | `app/.../ocr/TesseractOcrRecognizer.kt` |
| ML Kit Latin | `app/.../ocr/LatinOcrRecognizer.kt` |
| Tessdata assets | `app/src/main/assets/tessdata/*.traineddata` |
| Benchmark harness | `app/.../ocr/OcrBenchmark.kt` |
| Synthetic runner | `app/.../ocr/OcrBenchmarkRunner.kt` |
| UI picker | `app/.../ui/components/OcrLanguagePicker.kt` |
