# ClearScan GitHub Release Updates

ClearScan uses an **opt-in in-app updater** that reads `distribution/version.json` from the `main` branch on GitHub and downloads APKs from **GitHub Releases**.

## How it works

1. User opens **Settings → App updates → Check for updates**.
2. App fetches:
   `https://raw.githubusercontent.com/SuvenSeo/ClearScan/main/distribution/version.json`
3. If `versionCode` is greater than the installed app, a dialog shows release notes.
4. On confirm, Android `DownloadManager` downloads the APK.
5. When complete, the system installer opens (user may need to allow **Install unknown apps** for ClearScan once).

No background polling. No analytics. No forced updates.

## Publish a new build

### 1. Bump app version

In `app/build.gradle.kts`:

```kotlin
versionCode = 3        // must increase every release
versionName = "0.2.1"
```

### 2. Build APK

```powershell
.\gradlew :app:assembleDebug
```

Copy output to a release-friendly name, e.g. `ClearScan-v0.2.1-debug.apk`.

### 3. Create GitHub Release

1. Go to https://github.com/SuvenSeo/ClearScan/releases/new
2. Tag: `v0.2.1` (or your tag name)
3. Upload `ClearScan-v0.2.1-debug.apk` as a release asset
4. Publish release

### 4. Update `distribution/version.json`

Commit to `main` **after** the release asset is live:

```json
{
  "versionCode": 3,
  "versionName": "0.2.1",
  "apkUrl": "https://github.com/SuvenSeo/ClearScan/releases/download/v0.2.1/ClearScan-v0.2.1-debug.apk",
  "releaseNotes": "What changed in plain language.",
  "minVersionCode": 1
}
```

**Important:** `apkUrl` must be the direct download URL for the release asset (HTTPS). `versionCode` must match `app/build.gradle.kts`.

### 5. Test on device

1. Install an older APK (lower `versionCode`).
2. Settings → Check for updates.
3. Confirm download and install.

## Signing

- **Debug builds:** fine for friends; each machine’s debug key differs — updates only work if the new APK is signed with the **same key** as the installed app.
- **Release builds:** use one release keystore for all distributed APKs.

For a stable friend-test channel, pick one debug/release keystore and reuse it.

## Privacy

- Update checks hit GitHub only when the user taps the button.
- Download uses Android’s system download notification.
- Document scans are never uploaded as part of this flow.
