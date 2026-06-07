# Privacy Architecture

ClearScan treats scanned documents as sensitive by default.

## Defaults

- No account required.
- No ad SDKs.
- No hidden analytics.
- No cloud sync by default.
- Scanner output is copied into app-private storage.
- OCR runs locally against saved page images.
- Searchable PDFs are generated locally before explicit export.
- Android system backup excludes stored documents.

## Permission Strategy

- Use ML Kit Document Scanner for capture instead of owning a raw camera permission in Phase 1.
- Use system import flows where possible instead of broad media access.
- Add permissions only when a feature cannot work without them.

## Storage Strategy

- Store documents under `filesDir/documents`.
- Store metadata in a local `index.json` until Room is introduced.
- Store OCR text and searchable PDF paths in the local index.
- Move to encrypted file storage and Android Keystore-managed keys before release.
- Add biometric/device-credential app lock before public beta.

## Network Strategy

- The core scanner must work offline.
- Export/sync/upload requires an explicit user action.
- Any future AI, OCR server, or cloud sync path must show a clear opt-in boundary.

## Release Gate

Before Play Store submission:

- Verify Data Safety answers against the actual code.
- Confirm no tracker/ad SDKs are present.
- Confirm no billing/subscription SDKs are present.
- Run a proxy/network test while scanning and browsing the local library.
- Confirm backup exclusions and app-lock behavior on a real device.
