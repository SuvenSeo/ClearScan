# ClearScan Privacy Policy

**Last updated:** June 2026

ClearScan is a local-first document scanner for Android. This policy describes what data the app handles and how.

## Summary

- Scanned documents, OCR text, and exports stay on your device by default.
- ClearScan does not require an account.
- ClearScan does not include ads or third-party analytics SDKs.
- Network access is used only when you explicitly enable features that need it (app updates, self-host export).

## Data we process on your device

| Data | Purpose | Leaves device? |
|------|---------|----------------|
| Camera/scanner images | Document capture | Only when you share or export |
| Page images and PDFs | Storage and PDF tools | Only when you share, export, or self-host |
| OCR text | Search and searchable PDFs | Only when you share or export |
| Biometric/device credential | Optional vault lock | Never |
| Export audit log | Privacy dashboard | Never (local JSON) |

## Encryption

- Document files are encrypted at rest using Android Keystore AES-GCM.
- Optional encrypted backups (`.csbak`) use either device-bound keys or a passphrase you choose.

## Permissions

- **Camera** — used by ML Kit Document Scanner and page-turn capture when you start a scan.
- **Internet** — used for optional app update checks and self-host upload when configured.
- **Install packages** — used only when you choose to install an in-app update APK.

## Self-host export

If you configure WebDAV or paperless-ngx in Settings, ClearScan uploads files only when you tap upload on a document. Credentials are stored locally in app preferences.

## Children's privacy

ClearScan is not directed at children under 13. We do not knowingly collect personal information from children.

## Changes

We may update this policy as features evolve. Material changes will be noted in release notes.

## Contact

Project: [github.com/SuvenSeo/ClearScan](https://github.com/SuvenSeo/ClearScan)
