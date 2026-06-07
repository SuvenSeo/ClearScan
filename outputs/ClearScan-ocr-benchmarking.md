# Sinhala/Tamil OCR Benchmarking

ClearScan now includes a CER/WER scoring harness for Sinhala and Tamil OCR evaluation.

## What Is Implemented

- `OcrBenchmarkCase` for expected text and OCR output.
- Character Error Rate (CER).
- Word Error Rate (WER).
- Per-language summary for Sinhala and Tamil.
- Unit tests for exact and mismatched samples.
- In-app self-check button to confirm the harness is available.

## What Still Needs Real Data

The harness is ready, but accuracy claims require a real local test corpus:

- At least 25 Sinhala document scans.
- At least 25 Tamil document scans.
- Ground-truth text manually typed from each scan.
- Separate results for printed text, handwritten text, receipts, forms, and low-light scans.

## Benchmark Rule

Do not claim Sinhala/Tamil OCR support until a chosen OCR engine is evaluated against the corpus and the report is updated with measured CER/WER.

## Candidate Engines

- Current ML Kit text recognizer for baseline behavior.
- Tesseract with Sinhala/Tamil traineddata for offline OCR.
- PaddleOCR or another local model if Android runtime size and speed are acceptable.

Cloud OCR should stay optional and explicit; it must never become a hidden upload path.
