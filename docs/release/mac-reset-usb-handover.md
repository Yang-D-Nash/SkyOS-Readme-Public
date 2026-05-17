# SkyOS USB + Mac Reset Handover

Use this checklist before wiping or resetting the release Mac.

## 1) Goal

Create a single USB handover package that allows release work to continue on another machine without
copying secrets into git.

## 2) What Must Be On USB

Create a folder on USB, for example: `SkyOS-Release-Handover-2026-05-08/`

Required content:

- `docs/release-checklist.md`
- `docs/release/store-upload-runbook.md`
- `docs/release/mac-reset-usb-handover.md`
- `fastlane/Fastfile`
- `fastlane/README.md`
- latest Android release artifacts:
  - `androidApp/build/outputs/bundle/release/androidApp-release.aab`
  - `androidApp/build/outputs/apk/release/androidApp-release.apk`
- iOS release export inputs/outputs for current run (if present):
  - `build/ios/*.xcarchive`
  - `build/ios/*.ipa`
  - `build/ios/ExportOptions-*.plist`
- a plain text `handover-notes.txt` with:
  - current iOS build number / Android versionCode
  - what is uploaded already
  - what is still open
  - who owns final App Store / Play publish click

## 3) Secrets Handling (Do Not Commit)

Keep these out of git and copy securely (USB only if encrypted and physically controlled):

- App Store Connect API key `.p8` (used by `ASC_KEY_PATH`)
- Play Console service-account JSON (used by `SUPPLY_JSON_KEY`)
- any local `.env*`, signing keys, keystores, or credential exports

After transfer, verify those files are NOT inside repository tracked paths.

## 4) iOS Finalization Steps (Current Open Item)

1. Confirm `CURRENT_PROJECT_VERSION` = `10030`.
2. Archive and local App Store IPA export are complete for build `10030` at `build/ios/SkyOS-1.0.0-10030-20260512.xcarchive` and `build/ios/export-10030/SkyOS.ipa`; previous uploaded build `10029` is superseded by the 2026-05-12 release-hardening changes.
3. Upload is complete for build `10030` (`Upload succeeded` on 2026-05-12 13:15 CEST).
4. Release owner confirmed the app is out at 2026-05-12 13:24 CEST.
5. Continue post-release monitoring and record any App Store Connect warnings in `docs/release/store-upload-runbook.md`.

## 5) Android Status

- Android `10033` is built, verified, uploaded to Play Internal Testing as a draft, and release owner confirmed the app is out at 2026-05-12 13:24 CEST.
- Previous uploaded draft `10032` is superseded by the 2026-05-12 release-hardening changes.

## 6) Pre-Reset Validation

- USB opens on a second machine and contains expected files.
- Checksums for AAB/APK are recorded in runbook.
- Handover notes include current blockers and next exact click.
- Credentials are available to release owner outside the Mac keychain-only context.

## 7) Safe Reset Sequence

1. Confirm iOS upload state and notes are updated.
2. Confirm USB handover package integrity.
3. Sign out of App Store Connect, Play Console, Firebase CLI, and other release accounts.
4. Remove local credential leftovers not needed post-reset.
5. Proceed with Mac reset.
