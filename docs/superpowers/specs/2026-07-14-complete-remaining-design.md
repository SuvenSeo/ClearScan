# ClearScan v0.2.4 — Complete Remaining Work

Approved scope from user directive “do everything” after v0.2.3 deferred-gap list.

## Goals

1. Localize remaining user-facing UI strings (en/si/ta) for secondary screens and snackbars.
2. Add real PNG scan fixtures for Sinhala/Tamil OCR corpus (25+ each) with `imageFile` wired.
3. Wire folder rename/delete UI; shrink ViewModel façade leftovers.
4. Vault UX edges: auto-lock on background, lockout recovery messaging, wire biometric state to lock screen.
5. Ship as v0.2.4 with tests green and GitHub release.

## Approaches considered

| Approach | Trade-off | Choice |
|----------|-----------|--------|
| A. Full sealed MessageId refactor for snackbars | Cleanest i18n; large blast radius | Partial — string resources now; sealed types later |
| B. Localize Compose first, leave ViewModel English | Faster UI parity | No — snackbars are user-facing |
| C. Synthetic rendered PNGs for OCR corpus | Not camera captures but unblocks image path | Yes — render Unicode text to PNG; docs note “synthetic scan fixtures” until real photos land |

## Design

### Localization
- Expand `values/strings.xml`, `values-si`, `values-ta`.
- Wire `stringResource()` in Settings, Privacy, DocumentDetail, Annotator, PageEditor, dialogs, library components.
- ViewModels: accept `android.content.Context`/`Application` or string providers; prefer `getApplication<Application>().getString(R.string…)` via AndroidViewModel pattern where already available, else resolve at call site in MainActivity/ClearScanApp when showing snackbars.

### OCR corpus images
- Extend `tools/ocr-corpus/generate-synthetic-corpus.py` (or sibling script) to emit PNGs with DejaVu/Noto fonts.
- Set `imageFile` on each entry; keep `actualText` for JVM CER/WER.
- Update corpus check script to require PNGs exist when `imageFile` set.

### Folders UI
- Long-press / overflow on folder chip → Rename + Delete dialogs.
- Thread `onRenameFolder` / `onDeleteFolder` through ClearScanApp.

### Vault
- Auto-lock on `ON_STOP` when vault enabled.
- Pass `hasBiometric` / auth errors into `VaultLockScreen`.
- When biometrics unavailable, show clear recovery copy (still require device credential if enrolled).

### Non-goals (explicit)
- Desktop/web CamScanner parity
- Real camera-captured corpus photography
- Sealed snackbar message IDs (follow-up)
