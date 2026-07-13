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
- PDF merge/split/rotate/sign/redact/password operations generate new local copies and do not overwrite the original scan.
- Vault mode gates the scan library behind Android biometric/device credential prompt.
- Document page images and PDFs are encrypted at rest with Android Keystore AES/GCM (`.enc` blobs under `filesDir/documents`).
- Readable copies are decrypted only into app-private cache for OCR, PDF tools, and explicit export.
- Android system backup (`allowBackup=false`) excludes stored documents; users can export an encrypted `.csbak` backup via SAF.

## Permission Strategy

- Use ML Kit Document Scanner for capture instead of owning a raw camera permission in Phase 1.
- Use system import flows where possible instead of broad media access.
- Add permissions only when a feature cannot work without them.

## Storage Strategy

- Store encrypted document blobs under `filesDir/documents/{documentId}/`.
- Store document metadata in `index.json` and mirror it to Room (`ScanDatabase`) on every write (JSON + Room dual-write).
- Store folders in `folders.json` and mirror to Room on every write.
- Store OCR text and searchable PDF paths in the local index (paths reference encrypted blobs).
- Legacy plaintext files are migrated to encrypted blobs on first load after upgrade.
- Export audit events are stored locally in `filesDir/export-audit.json`.

## Encryption And Backup

- **At rest:** `VaultCrypto` generates a hardware-backed AES/GCM key in Android Keystore (`clearscan_vault_aes`). `EncryptedFileStore` wraps each file with a `CSC1` header, IV, and ciphertext.
- **Readable cache:** Decrypted files live under `cacheDir/vault-read/{documentId}/` and are invalidated when the encrypted source changes.
- **Explicit backup:** Settings → Backup exports a `.csbak` file (SAF `CreateDocument`). The archive is a zip of `index.json`, `folders.json`, and encrypted blobs, then encrypted again with the vault key. Restore replaces local storage on the same device (Keystore-bound).
- **Passphrase backup (optional):** Users can protect a backup with a passphrase instead of device-bound Keystore encryption. Passphrase backups use PBKDF2 + AES/GCM and can be restored on another device when the same passphrase is supplied.

## Network Strategy

- The core scanner must work offline.
- Export/sync/upload requires an explicit user action.
- Any future AI, OCR server, or cloud sync path must show a clear opt-in boundary.

## Privacy Dashboard

Settings → Privacy dashboard surfaces:

- Offline / no background network policy
- App-private storage location
- Encryption-at-rest health (Keystore round-trip)
- System backup exclusion status
- Ad SDK classpath scan (common monetization SDK markers)
- Export audit log (share/export actions only)

## Release Gate

Before Play Store submission:

- Verify Data Safety answers against the actual code.
- Confirm no tracker/ad SDKs are present.
- Confirm no billing/subscription SDKs are present.
- Run a proxy/network test while scanning and browsing the local library.
- Confirm backup exclusions, encryption migration, and app-lock behavior on a real device.
- Confirm encrypted backup export/restore on the same device.
- Confirm generated password-protected PDFs open with the password in an external PDF viewer before release.
