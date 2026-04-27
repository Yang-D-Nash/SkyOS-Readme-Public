# Changelog

## 1.0.0 Release Readiness - 2026-04-27

- Verified local release gates for the current candidate: shared tests, Android lint/metadata compile, Functions tests, Firestore/Storage rules tests, Detekt, Android artifact verification, and iOS Release simulator build.
- Documented current build identity: iOS `1.0.0` build `10007`, Android `1.0.0` versionCode `10015`.
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
