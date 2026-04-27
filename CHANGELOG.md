# Changelog

## 1.0.0 Release Readiness - 2026-04-27

- Verified local release gates for the current candidate: shared tests, Android lint/metadata compile, Functions tests, Firestore/Storage rules tests, Detekt, Android artifact verification, and iOS Release simulator build.
- Documented the pre-reupload build identity: iOS `1.0.0` build `10007`, Android `1.0.0` versionCode `10015`.
- Prepared the next iOS upload by bumping `CURRENT_PROJECT_VERSION` from `10007` to `10008` after App Store Connect rejected a redundant re-upload of build `10007`.
- Archived iOS build `10008` locally and verified the archive identity as `SkyOS` / `com.skydown.ios` / `1.0.0`.
- Uploaded iOS build `10008` to App Store Connect; package processing started, with only vendor binary-framework dSYM warnings reported.
- Fixed the iOS Music Studio entry so `NICMA MUSIC` and `NICMA STUDIO` use separate ArtistPage states like Android.
- Hardened the iOS Agent mode entry so tapping Agent mounts the screen before live task/note observers attach; build `10009` was superseded before successful upload, and iOS build `10010` was archived, identity-checked, and uploaded to App Store Connect.
- Bumped Android to versionCode `10016` for the next tester build after `10015` went online, then rebuilt and verified the Play AAB.
- Added a release identity preflight to verify Android, iOS, Fastlane, Firebase client files, and the runbook before store uploads.
- Gated Android UI-test launch extras to debuggable builds so release builds cannot be started into mock data or a local fixture user through exported Activity extras.
- Exported Google Play-ready Android phone screenshots at `1242x2424` and Play listing graphics (`512x512` icon, `1024x500` feature graphic).
- Exported iPad App Store screenshots at `2064x2752` after making the screenshot test iPad top-tab aware.
- Removed the launch-time push permission prompt; notification permission now refreshes silently until the user explicitly requests it.
- Clarified that public store rollout remains `no-go` until App Store Connect/TestFlight confirmation, Google Play API validation, final asset upload/mapping, final URLs/legal approval, and real-device smoke are complete.
- Expanded root README, documentation index, release checklist, and screenshot docs with release-critical status and next gates.

## 1.0.0 - 2026-04-24

- Established SkyOS as the version 1 product identity across project metadata, Android versioning, iOS display metadata, and backend package metadata.
- Added a clean functions build script (`npm run build --prefix functions`) for server-side syntax validation.
- Kept existing Firebase callable names and mobile package identifiers for compatibility with the configured Firebase project.
- Added SkyOS-prefixed release signing environment variables while preserving legacy `SKYDOWN_UPLOAD_*` support for existing local setups.
- Stabilized Android lint for V1 by fixing code-level lint errors and treating the existing partial localization backlog as warnings.
- Documented V1 build commands and environment expectations, including release signing and backend secrets.
