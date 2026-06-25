# Play Store & F-Droid Checklist

## Store listing assets

- [ ] App title: `ClearScan` (`fastlane/metadata/android/en-US/title.txt`)
- [ ] Short description (80 chars max)
- [ ] Full description
- [ ] Feature graphic 1024×500
- [ ] Phone screenshots (min 2)
- [ ] Privacy policy URL (host `docs/PRIVACY_POLICY.md` or project page)

## Policy compliance

- [ ] `allowBackup=false` in manifest
- [ ] No ad SDKs (`scripts/check-free-policy.ps1` passes)
- [ ] Data safety form: documents processed on-device; optional network for self-host only
- [ ] Camera permission justified (document scanning)
- [ ] Target API meets Play requirements (`targetSdk` in `app/build.gradle.kts`)

## Technical

- [ ] Signed release APK or AAB
- [ ] ProGuard rules tested (`release` minify enabled)
- [ ] 64-bit ABI support (default with modern AGP)
- [ ] Content rating questionnaire completed

## F-Droid

- [ ] Reproducible build instructions in README
- [ ] No proprietary blobs except Google Play Services ML Kit (document scanner) — disclose in metadata
- [ ] `fastlane/metadata/android/en-US/` descriptions match app behavior
- [ ] Anti-feature: No cloud dependency for core scanning

## Post-launch

- [ ] Update `distribution/version.json` for in-app updates
- [ ] GitHub Release with APK and checksum
- [ ] Monitor crash reports if enabled later (currently none by design)
