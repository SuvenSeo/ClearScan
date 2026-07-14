# ClearScan OCR Corpus

Labeled Sinhala and Tamil scan images plus ground-truth text for measuring CER/WER before claiming production OCR quality.

## Directory Layout

```
tools/ocr-corpus/              # Authoring docs and schema reference (this folder)
app/src/test/resources/ocr-corpus/   # JVM benchmark corpus (checked into git)
  index.json                   # Lists entry JSON files
  *.json                       # One metadata file per labeled sample
  *.png                        # Rendered (or camera) scan image paired via imageFile
```

## Regenerating Synthetic Fixtures

```bash
python3 tools/ocr-corpus/generate-synthetic-corpus.py   # JSON ground truth
python3 tools/ocr-corpus/render-corpus-images.py         # PNG scan fixtures + imageFile
```

Every entry in the checked-in corpus must include `imageFile` pointing at an existing PNG. `scripts/check-ocr-corpus.ps1` enforces this in CI.

## Adding a Labeled Sample

1. Copy a scan PNG into `app/src/test/resources/ocr-corpus/` (or keep large assets out of git and load from a local path during device QA).
2. Create a JSON entry beside it. Use `sample-entry.json` in this folder as the schema reference.
3. Type the ground-truth text into `expectedText` from the scan (not from OCR output).
4. For JVM scoring without running Tesseract, set `actualText` to a prior OCR run or a deliberate mismatch for regression tests.
5. For on-device OCR scoring, omit `actualText` and set `imageFile` to the PNG file name; run the in-app self-check or a future device harness.
6. Register the JSON file in `index.json` under `entries`.

## Entry Schema

| Field | Required | Description |
|-------|----------|-------------|
| `id` | yes | Stable sample identifier (used in benchmark reports). |
| `language` | yes | `sinhala` or `tamil`. |
| `category` | no | e.g. `synthetic-print`, `receipt`, `form`, `handwritten`, `low-light`. |
| `description` | no | Human note about capture conditions. |
| `expectedText` | yes | Manually typed ground truth. |
| `actualText` | no | OCR output to score on JVM; omit when scoring live on device. |
| `imageFile` | yes (CI) | PNG file name in the same corpus folder; required for checked-in corpus. |

## Running the Corpus Harness

JVM unit tests load entries from the test classpath:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.ardeno.clearscan.ocr.OcrCorpusBenchmarkTest"
```

Kotlin API:

```kotlin
val entries = OcrCorpusBenchmark.loadFromClasspath(classLoader)
val metrics = OcrCorpusBenchmark.evaluate(entries)
println(OcrCorpusBenchmark.summary(metrics))
```

## Corpus Targets (Phase 4)

Before claiming Sinhala/Tamil parity with production scanners:

- At least 25 Sinhala document scans with ground truth.
- At least 25 Tamil document scans with ground truth.
- Categories: printed text, handwriting, receipts, forms, low-light.

See [docs/OCR_BENCHMARKING.md](../../docs/OCR_BENCHMARKING.md) for measured results and release criteria.
