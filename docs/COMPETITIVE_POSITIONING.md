# ClearScan Competitive Positioning

This document explains why ClearScan should be positioned as a free CamScanner-style alternative without making weak or misleading claims.

## Short Positioning

ClearScan is better for users who want a scanner that does not monetize their documents through ads, subscriptions, watermarks, forced accounts, or forced cloud workflows.

The message:

> A document scanner should scan your documents, not rent your own paperwork back to you.

## What CamScanner Already Does Well

CamScanner is mature and feature-rich. Its official listings describe scanning, OCR, PDF/JPG/TXT export, folders, signatures, password protection, printing, sharing, cloud upload, and document conversion. ClearScan should not claim CamScanner lacks these basic scanner features.

Sources:

- Google Play listing: https://play.google.com/store/apps/details?id=com.intsig.camscanner
- App Store listing: https://apps.apple.com/us/app/camscanner-pdf-scanner-app/id388627783
- Official site: https://www.camscanner.com/

## The ClearScan Advantage

| Area | CamScanner | ClearScan Direction |
| --- | --- | --- |
| Ads | Google Play listing says "Contains ads". | No ads in the scanner core. |
| Subscriptions | App Store listing describes weekly, monthly, quarterly, and annual subscriptions for unlimited access. | No subscriptions for core features. |
| Watermarks | Historically common scanner-app limitation and a user pain point. | No watermark on exports. |
| Account pressure | Many scanner products push cloud/account workflows. | No account required for local scanning. |
| Cloud | CamScanner supports cloud upload/sync flows. | Local-first by default; upload only by explicit export/sync action. |
| Privacy model | Closed commercial app. | Publish a clear privacy architecture and keep the core offline-first. |
| AI features | Commercial cloud AI/conversion features can become premium gates. | Use local models, self-hosting, donations, or BYO keys before adding cloud-heavy AI. |
| Local language focus | Broad global product. | Sinhala/Tamil OCR evaluation is a planned differentiator. |

## Implemented Evidence So Far

- Android-native scan/import flow.
- Private app-storage scan library.
- Local OCR text extraction over saved page images.
- OCR text search.
- Locally generated searchable PDF export.
- Free local PDF editor actions: merge, split, rotate, typed signature, header redaction, and password-protected PDF copy.
- Biometric/device-credential vault gate with optional biometric-bound Keystore keys.
- Encrypted metadata at rest (`index.json`, Room `json_payload`) via `MetadataCrypto`.
- Sinhala/Tamil OCR benchmark harness with 25+ labeled corpus entries per language (CER/WER scoring).
- Compose UI localization foundation for English, Sinhala (si), and Tamil (ta).
- Android share-sheet export with no cloud account.
- Delete from local storage.
- Folders, tags, favorites, batch library actions, and duplicate detection.
- Free-policy script for ad, billing, and monetization SDK checks.
- ViewModel decomposition: `LibraryViewModel`, `SettingsViewModel`, and domain processors for OCR/PDF/capture.

## Deep Competitive Research

For product architecture, monetization, feature parity matrix, and user-pain-point analysis, see [CAMSCANNER_RESEARCH.md](CAMSCANNER_RESEARCH.md).

## Features ClearScan Should Keep Free

These are the feature groups that should never become subscription-only in ClearScan:

- Multi-page scanning.
- PDF/JPG export.
- OCR text extraction.
- Searchable PDFs.
- Text search.
- Folders and tags.
- Merge, split, reorder, rotate, and compress.
- Signature, annotation, highlight, and redaction.
- Password-protected PDF export.
- Import from image/PDF.
- App lock and encrypted storage.
- Local backup and restore.

## Features That Are Not Unique, But Must Be Free

Some of these features exist in CamScanner or other scanner apps. The difference is not invention; the difference is the business model.

- OCR.
- Folders.
- Signature.
- PDF conversion.
- Password protection.
- Cloud export.
- PDF editing.

ClearScan wins if those stay free, offline-friendly, and simple.

## Claims To Avoid

Avoid these unless verified by current evidence:

- "CamScanner is unsafe today."
- "CamScanner uploads all documents."
- "CamScanner lacks OCR."
- "CamScanner lacks PDF tools."
- "ClearScan has better OCR accuracy" before benchmarks exist.
- "ClearScan supports Sinhala/Tamil OCR" before the models are implemented and tested.

Historical note: Kaspersky reported a malicious module in CamScanner's Android app in 2019 and connected it to an advertising module. That is useful context for why ClearScan avoids ad SDKs, but it should not be presented as a current claim about the modern CamScanner app.

Source: https://www.kaspersky.co.uk/blog/camscanner-malicious-android-app/16598/

## Store Listing Draft

ClearScan is a private document scanner built to stay free.

Scan documents, save PDFs, extract text, organize files, and export clean documents without ads, watermarks, subscriptions, or forced accounts. Your scans stay local by default. Cloud upload only happens when you choose to export or sync.

Core promise:

- Free scanner.
- No ads.
- No subscriptions.
- No watermark.
- No account required.
- Offline-first.
- Privacy-first.
