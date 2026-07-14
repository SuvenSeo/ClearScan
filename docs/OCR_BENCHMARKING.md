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
- `OcrCorpusBenchmark` loads labeled entries from `app/src/test/resources/ocr-corpus/` when present.
- `OcrBenchmarkRunner` synthetic print benchmark (rendered text → OCR → CER/WER).
- In-app **Run self-check** (Settings → Developer) runs the synthetic engine benchmark on-device.

## Measured Results

### Harness unit tests (JVM)

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.ardeno.clearscan.ocr.OcrBenchmarkTest"
.\gradlew.bat :app:testDebugUnitTest --tests "com.ardeno.clearscan.ocr.OcrCorpusBenchmarkTest"
```

`OcrBenchmarkTest` validates CER/WER math. `OcrCorpusBenchmarkTest` loads placeholder Sinhala/Tamil entries from the test classpath and scores them.

## Labeled Scan Corpus Workflow

Authoring docs and schema: [tools/ocr-corpus/README.md](../tools/ocr-corpus/README.md).

1. Add a PNG scan and JSON entry under `app/src/test/resources/ocr-corpus/`.
2. Register the JSON file in `ocr-corpus/index.json`.
3. Set `expectedText` to manually typed ground truth.
4. For JVM scoring, set `actualText` to a stored OCR run (or omit it for future on-device scoring via `imageFile`).
5. Run `OcrCorpusBenchmarkTest` or call `OcrCorpusBenchmark.evaluateClasspathCorpus(classLoader)` from tooling.

Synthetic corpus checked in (Phase 4 target met for JVM harness):

| Language | Entries | Categories |
|----------|---------|------------|
| Sinhala | 27 | receipt, invoice, form, synthetic-print, handwritten, low-light |
| Tamil | 27 | receipt, invoice, form, synthetic-print, handwritten, low-light |

Representative samples:

| ID | Language | Category | Purpose |
|----|----------|----------|---------|
| `sinhala-synthetic-01` | Sinhala | synthetic-print | Exact-match harness smoke |
| `tamil-synthetic-01` | Tamil | synthetic-print | Deliberate mismatch regression |
| `sinhala-receipt-01` | Sinhala | receipt | Grocery receipt ground truth |
| `tamil-invoice-04` | Tamil | invoice | Partial OCR mismatch regression |

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

### Synthetic JVM corpus with rendered scan PNGs (Phase 4 — complete)

The test classpath corpus includes **27 Sinhala** and **27 Tamil** labeled JSON entries with `expectedText` ground truth, `actualText` for JVM CER/WER scoring, and a rendered **PNG scan fixture** via `imageFile` (Unicode text → A4-like PNG using Noto Sans Sinhala/Tamil).

Regenerate text entries via `tools/ocr-corpus/generate-synthetic-corpus.py`.
Render/update PNGs via `tools/ocr-corpus/render-corpus-images.py`.
CI validates `imageFile` presence through `scripts/check-ocr-corpus.ps1`.

Categories covered: printed text, handwriting, receipts, invoices, forms, and low-light document descriptions.

### Camera-captured scan corpus (future)

Production OCR accuracy claims still benefit from labeled **real camera** scan PNGs paired with the same JSON schema:

- Replace or supplement synthetic rendered fixtures with device camera captures.
- Score on-device via Tesseract using `imageFile` without relying on stored `actualText`.

Add entries via [tools/ocr-corpus/README.md](../tools/ocr-corpus/README.md).

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
| Corpus loader | `app/.../ocr/OcrCorpusBenchmark.kt` |
| Corpus assets | `app/src/test/resources/ocr-corpus/` |
| Corpus renderer | `tools/ocr-corpus/render-corpus-images.py` |
| Synthetic runner | `app/.../ocr/OcrEngine.kt` (`OcrBenchmarkRunner`) |
| UI picker | `app/.../ui/components/OcrLanguagePicker.kt` |
