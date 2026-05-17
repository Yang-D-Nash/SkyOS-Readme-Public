# 22kydwn Store Upload Runbook

Last updated: 2026-05-17 CEST (release rebrand pass: visible app label and public copy changed from SkyOS to 22kydwn; bundle IDs remain stable.)
Owner: Release Engineering

Single source of truth for current release state: this file.
If any other release note conflicts with this runbook, follow this runbook.

## Build Identity

### iOS
- Bundle ID: `com.skydown.ios`
- Display Name: `22kydwn` (from `SkydownApp-Info.plist`)
- Version: `1.0.0`
- Build: `10032` (current project version; premium production calculator/support funnel release candidate)
- Team ID: `F3BNLG6L7P`

### Android
- Application ID: `com.nash.skyos`
- App Label: `22kydwn`
- versionName: `1.0.0`
- versionCode: `10036`
- Play Billing Library: `8.3.0`

## Current Release Status

- Branding pass complete (2026-05-17 CEST): public product name **22kydwn**; video **Skydown Entertainment**; music **ZweiZwei**; merch line artwork unchanged.
- Current upload candidates: iOS **22kydwn 1.0.0 (10032)** and Android **22kydwn 1.0.0 (10036)**. Supersede any older SkyOS-labelled store binaries before wider rollout.
- `./scripts/release_identity_check.sh` passes against the build identity block above.
- Store copy source: `docs/store-listing.md` (App Store + Play descriptions and keywords).
- Previously shipped store identities before this pass may still show older labels until new binaries are approved.
- Android hotfix candidate: versionCode `10034` corrects the German Payments provider copy (`Stripe`, not `Streifen`) and German close labels. It was uploaded to Google Play internal testing as a draft at 2026-05-12 14:28 CEST, later completed on the internal track, and is not production/public.
- Google Play API status at 2026-05-12 15:15 CEST: internal track contains `10034` with status `completed`; production, beta, and alpha tracks have no releases. The Samsung Fold installed Play package `versionCode=10034` at 2026-05-12 17:03 CEST and passed the Play-installed hotfix smoke.
- Previous store uploads iOS `10029` and Android `10032` are superseded and must not be selected for tester/public rollout.
- Post-release mode is active: monitor crashes, first-session analytics, purchase/restore behavior, auth, AI, Music, Video, Shop, Orders, Settings, push permission, and reminders from installed store builds.

## Fastlane: store upload lanes

Definitions live in `fastlane/Fastfile`. Common prerequisite: repo root shell, **`fastlane`** on `PATH`, and a **fresh release AAB** at `androidApp/build/outputs/bundle/release/androidApp-release.aab` (after `./scripts/android_release_clean_build.sh` or equivalent). Every Android lane runs `scripts/verify_android_release_artifacts.sh` before talking to Play.

Discover lanes anytime:

```bash
cd "$REPO_ROOT" && fastlane lanes
```

### Google Play — internal testing (`internal` track, draft release)

Uses `SUPPLY_JSON_KEY` pointing at your Play Console **service-account JSON file** path (never commit that file).

```bash
export SUPPLY_JSON_KEY="/secure/path/play-service-account.json"
cd "$REPO_ROOT"

fastlane android validate_android_internal   # validates only — no upload
fastlane android upload_android_internal      # uploads AAB as draft to internal testing
```

### Google Play — production track (soft / public rollout)

Production lanes default to **`ANDROID_PLAY_TRACK=production`** and **`ANDROID_PLAY_RELEASE_STATUS=draft`** so the binary lands as a **Play Console draft** on the production track; you promote and publish from the console unless you deliberately override.

```bash
export SUPPLY_JSON_KEY="/secure/path/play-service-account.json"
cd "$REPO_ROOT"

fastlane android validate_android_production
fastlane android upload_android_production
```

Optional overrides (see `Fastfile` for behavior):

| Variable | Role |
|---------|------|
| `ANDROID_PLAY_TRACK` | Default `production`; set only if you use a named custom track deliberately. |
| `ANDROID_PLAY_RELEASE_STATUS` | Default `draft`; set to `completed` or `inProgress` only when you intend an automated rollout state (know what you are doing in Play Console). |
| `ANDROID_PLAY_ROLLOUT` | Phased rollout fraction (example `0.12` for **12%)** — only honoured when status is **`completed`** or **`inProgress`**. |

Still skips metadata/screenshots/changelogs in Fastlane (`skip_upload_*` flags); listings remain edited in Play Console unless you extend the lane.

### App Store Connect — binary upload via Fastlane (optional)

Historical primary path remains **`xcodebuild -exportArchive` → `destination=upload`** using your `ExportOptions-*.plist` (see Upload Status notes below).

Optional Fastlane lane **`upload_ios_app_store_connect`** uploads an **existing IPA** without submitting for review and without altering metadata/screenshots in App Store Connect.

```bash
cd "$REPO_ROOT"

export IOS_IPA_PATH="build/ios/22kydwn.ipa"                # repo-relative or absolute
export ASC_KEY_ID="YOUR_KEY_ID"
export ASC_ISSUER_ID="YOUR_ISSUER_UUID"
export ASC_KEY_PATH="/secure/path/AuthKey_XXXXXXXXXX.p8"   # Download once from ASC → Users and Access → Keys

fastlane ios precheck_ios_ipa
fastlane ios precheck_ios_asc_credentials   # optional: env + .p8 path only
fastlane ios upload_ios_app_store_connect   # uploads IPA; finalize review/submit in App Store Connect UI
```

`Fastfile` uses bundle id `com.skydown.ios` for this lane. Keep API keys **out of git** like `SUPPLY_JSON_KEY`.

### Public legal / support URLs (reference)

Hosting currently serves `site/` at **`https://skydown-a6add.web.app`** (privacy, terms, support). Paste the HTTPS URLs you expose **in storefront listings** under `docs/release/store-upload-runbook.md` → Metadata and URLs; do not leave placeholder domains live.

## Build Artifacts

- iOS archive: pending for the new `22kydwn` build `10032`. The previous `SkyOS` archive `build/ios/SkyOS-1.0.0-10031-20260516.xcarchive` is superseded for rollout.
- iOS UI test evidence: latest full fresh UI-test evidence remains `build/ios/Skydown-App-10018-ui-tests-fresh-20260428-170034.xcresult` and raw log `build/ios/Skydown-App-10018-ui-tests-fresh-20260428-170034.log`; build `10020` contains the refreshed keyboard dismissal and Agent new-conversation UX after that gate.
- iOS upload evidence: pending for build `10032`; previous build `10031` upload evidence remains valid only for the superseded SkyOS-labelled candidate.
- iOS upload status: PENDING for build `10032`. Build `10030` is the currently released/superseded baseline for this follow-up.
- Android AAB: pending rebuild for versionCode `10036`.
- Android APK: pending rebuild for versionCode `10036`.

## Upload Status

### iOS Upload Status
- Archive build: PENDING for current build `10032` after the 22kydwn rename pass.
- App Store Connect upload: PENDING for current build `10032`; previous build `10031` was uploaded at 2026-05-16 23:15 CEST with the old SkyOS label and should be superseded.
- Archive build: SUCCEEDED for current build `10030` at `build/ios/SkyOS-1.0.0-10030-20260512.xcarchive`.
- App Store IPA export: SUCCEEDED for current build `10030` at `build/ios/export-10030/SkyOS.ipa`.
- App Store Connect upload: SUCCEEDED for current build `10030` at 2026-05-12 13:15 CEST.
- Processing/status follow-up: release owner confirmed the app is out at 2026-05-12 13:24 CEST; continue monitoring App Store Connect status for build `10030`.
- Notes:
  - Target upload build `10032` contains the 22kydwn rename pass, premium Video production calculator, unified request/support sheet, and `submitAppRequest` backend callable.
  - Previous build `10031` upload used `build/ios/ExportOptions-app-store-upload-10031.plist` with `destination=upload` and `manageAppVersionAndBuildNumber=false`.
  - Target upload build `10030` is required because build `10029` does not contain the 2026-05-12 localization, notification, exact-alarm, and dependency hardening changes.
  - Upload used `build/ios/ExportOptions-app-store-upload-10030.plist` with `destination=upload` and `manageAppVersionAndBuildNumber=false`.
  - Keep `manageAppVersionAndBuildNumber=false` in ExportOptions for reproducible build-number control.
  - Xcode reported missing dSYMs for FirebaseFirestoreInternal, absl, grpc, grpcpp, and openssl_grpc during symbol upload; app package upload still succeeded.
  - Previous build `10026` uploaded successfully on 2026-05-06 for the artist page routing rollout.
  - Build `10026` supersedes `10025` after the Home-to-artist-page routing and exact MAVE/ThaDude public profile fixes.
  - Export/upload used `build/ios/ExportOptions-app-store-upload-10026.plist` with `manageAppVersionAndBuildNumber=false`; archive distribution output ended with `Upload succeeded` and `** EXPORT SUCCEEDED **`.
  - Xcode reported missing dSYMs for FirebaseFirestoreInternal, absl, grpc, grpcpp, and openssl_grpc binary frameworks during symbol upload; app package upload still succeeded.
  - Fresh iOS UI-test run on 2026-04-28 17:03-17:13 CEST passed on a newly created iPhone 17 simulator (`B8B0F386-8D3D-4390-81B6-2784B9E2FB20`, iOS 26.4.1 / 23E254a): 16 total tests, 12 passed, 4 intentionally skipped, 0 failures. Result bundle: `build/ios/Skydown-App-10018-ui-tests-fresh-20260428-170034.xcresult`.
  - During the fresh UI-test run Xcode emitted repeated `DebuggerLLDB.DebuggerVersionStore.StoreError error 0` / `no debugger version` warnings, but the test session completed and `xcodebuild` exited `0` with `** TEST SUCCEEDED **`; no LLDB/tooling hang reproduced.
  - Export/upload used `build/ios/ExportOptions-app-store-upload-10018.plist` with `manageAppVersionAndBuildNumber=false`; archive distribution metadata shows `uploadedBuildNumber` `10018` and upload event state `success`.
  - Xcode reported missing dSYMs for FirebaseFirestoreInternal, absl, grpc, grpcpp, and openssl_grpc binary frameworks during symbol upload; app package upload still succeeded.
  - Previous local archive path: `build/ios/SkyOS-1.0.0-10017-20260428.xcarchive`.
  - Export/upload used `build/ios/ExportOptions-app-store-upload-10010.plist` with `manageAppVersionAndBuildNumber=false`.
  - Build `10023` uploaded successfully on 2026-05-05, but was superseded by build `10024` for the legal/store-review date refresh.
  - Build `10022` uploaded successfully on 2026-05-04, but was superseded by build `10023` for the Music Hub duplicate-brand cleanup.
  - Build `10009` was archived and identity-checked locally on 2026-04-27, but its upload was intentionally stopped before success after the iOS Agent tap crash report; it was superseded by build `10010` at the time and by the current build `10029`.
  - Build `10008` uploaded successfully on 2026-04-27, but was superseded by build `10010` for the iOS Music Studio split fix and Agent tap hardening.
  - Export/upload for build `10008` used `build/ios/ExportOptions-app-store-upload-10008.plist` with `manageAppVersionAndBuildNumber=false`.
  - Xcode reported missing dSYMs for FirebaseFirestoreInternal, absl, grpc, grpcpp, and openssl_grpc binary frameworks during symbol upload; app package upload still succeeded.
  - A repeat upload for build `10007` was previously rejected as redundant, which indicates build `10007` already exists for version `1.0` / `1.0.0`.
  - Build `10007` was archived successfully on 2026-04-27.
  - The redundant-upload response for build `10007` means reusing the same build number will not work.
  - Build `10006` uploaded successfully on 2026-04-26 via `xcodebuild -exportArchive` with `destination=upload`.
  - Build `10005` uploaded successfully on 2026-04-26 via `xcodebuild -exportArchive` with `destination=upload`.
  - Xcode reported missing dSYMs for Firebase binary frameworks during symbol upload; app package upload still succeeded.
  - Previous build `10005` was superseded by build `10006` after the AI callable entitlement fix and smoke test.
  - Previous build `10004` was superseded by build `10005` for the public Video Hub player cleanup.
  - Previous build `10003` had resolved the Apple AppIcon alpha rejection (`90717`) by converting active AppIcon PNGs to opaque RGB.

### Google Upload Status
- Active upload candidate versionCode `10036` passed clean Android release gate locally at 2026-05-16 22:18 CEST.
- Fastlane `validate_android_internal` passed against Google Play for versionCode `10036` at 2026-05-16 22:20 CEST.
- Fastlane `upload_android_internal` uploaded versionCode `10036` to Google Play internal testing as a draft at 2026-05-16 22:28 CEST. This did not start a production rollout.
- Release AAB build: SUCCEEDED for versionCode `10031`, but Google Play rejected validation because that versionCode had already been used.
- Active hotfix candidate versionCode `10034` passed release identity check and clean Android release gate locally at 2026-05-12 14:24 CEST.
- Fastlane `validate_android_internal` passed against Google Play for versionCode `10034` at 2026-05-12 14:26 CEST.
- Fastlane `upload_android_internal` uploaded versionCode `10034` to Google Play internal testing as a draft at 2026-05-12 14:28 CEST. This did not start a production rollout.
- Fastlane/Google Play validate-only promotion attempt for existing versionCode `10034` (`track=internal`, `release_status=completed`, `version_code=10034`, `rollout=1`, `skip_upload_aab=true`) failed safely at 2026-05-12 14:51 CEST with `Only releases with status draft may be created on draft app`; no edit was committed.
- Google Play API track read at 2026-05-12 14:52 CEST found internal `10034` as `draft`, internal `10033` as `completed`, and no production/beta/alpha releases.
- Google Play API track read at 2026-05-12 15:15 CEST found internal `10034` as `completed` and no production/beta/alpha releases.
- Owner approved widening at 2026-05-12 17:28 CEST. Fastlane/Google Play validate-only promotion attempts for `10034` from `internal` to `production` (`completed` and `draft`) and `beta` (`completed`) all failed safely with `FAILED_PRECONDITION`; no edit was committed and no public/closed rollout was started.
- Play API detail read after the failed widening attempt returned default language `de-DE`, uploaded bundle `10034` with SHA-256 `ba9adc71291d734bbc12b13114a953b30c2b99bf5b0e3002ec7932eacd65dd92`, production country availability `{}`, and beta/alpha country availability synced with production. Finish the Play Console setup/review/country availability tasks before retrying public or closed-track widening.
- Active upload candidate versionCode `10033` passed clean release gate locally at 2026-05-12 12:11 CEST.
- Fastlane `validate_android_internal` passed against Google Play for versionCode `10033` at 2026-05-12 13:11 CEST.
- Fastlane `upload_android_internal` uploaded versionCode `10033` to Google Play internal testing as a draft at 2026-05-12 13:12 CEST.
- Active upload candidate versionCode `10032` passed clean release gate locally at 2026-05-08 16:53 CEST.
- Fastlane `validate_android_internal` passed against Google Play for versionCode `10032` at 2026-05-08 16:56 CEST.
- Fastlane `upload_android_internal` uploaded versionCode `10032` to Google Play internal testing as a draft at 2026-05-08 16:58 CEST.
- Play upload automation: WORKING via Fastlane `upload_android_internal` when `SUPPLY_JSON_KEY` points at the local Play service-account JSON.
- Notes:
  - Fastlane used `release_status: draft`, so this did not start a production rollout.
  - VersionCode `10035` contains the premium Video production calculator, live quote breakdown, support/request submission flow, and `submitAppRequest` backend integration.
  - VersionCode `10034` supersedes `10033` as an Android internal hotfix candidate for German Payments/close-label copy only. It is completed on the Play internal track and passed a Play-installed tester smoke. Owner approved widening, but Play API blocks production/closed promotion until console prerequisites are resolved.
  - VersionCode `10033` is required for the 2026-05-12 release-hardening follow-up; previous draft `10032` is superseded.
  - VersionCode `10032` supersedes `10031` for the premium brand release prep upload after Play reported `10031` as already used.
  - VersionCode `10031` contains the premium music artist rollout with owner CRUD + rename flows and canonical artist ordering.
  - VersionCode `10029` contains the Home-to-artist-page routing and exact MAVE/ThaDude public profile fixes.
  - VersionCode `10028` contains the Android AI generated-visual display hotfix: robust base64/data-URL decoding, ByteArray-backed image rendering, and a fullscreen viewer assertion in Android UI-test code.
  - Earlier versionCode `10027` was uploaded to the Play internal draft at 2026-05-05 23:37 CEST and is superseded by this fresh versionCode `10028` rollout.
  - VersionCode `10015` was previously confirmed online/visible by the release owner on 2026-04-27.

## 2026-05-06 Verification Log

1. Android AI visual client now decodes returned image payloads robustly, including data-URL-prefixed, wrapped, and URL-safe base64 payload variants.
2. Android AI message rendering now uses Coil `AsyncImage` directly from generated image bytes instead of eager `BitmapFactory` decoding in the message bubble.
3. Android fullscreen visual viewer now receives the same generated image byte payload and renders it through Coil.
4. Android UI-test component coverage now asserts that tapping a generated visual opens the fullscreen viewer.
5. Android debug Kotlin compile passed via `./gradlew :androidApp:compileDebugKotlin`.
6. Android debug instrumentation Kotlin compile passed via `./gradlew :androidApp:compileDebugAndroidTestKotlin`.
7. Android `versionCode` bumped to `10028` for the generated-visual display hotfix.
8. Android clean release gate passed for version `1.0.0` / versionCode `10028`; AAB and APK were rebuilt at 2026-05-06 00:27 CEST.
9. Android AAB SHA-256 for versionCode `10028`: `12ef439aa7f79022d8d701ab4d9015f29dd4e3b164e5cd6df8456235ec509d42`.
10. Android APK SHA-256 for versionCode `10028`: `2dc995d87e5792de8f721e83711cdbbf14c2876d554fa17b5b1f2b6c3a08901f`.
11. Fastlane `validate_android_internal` passed against Google Play for versionCode `10028` at 2026-05-06 00:29 CEST.
12. Fastlane `upload_android_internal` uploaded versionCode `10028` to Google Play internal testing as a draft at 2026-05-06 00:31 CEST.
13. Artist page routing hotfix was committed and pushed to `main`: Home Artist Page action now opens Yang D. Nash directly, and MAVE / ThaDude use exact provided Instagram and Spotify artist profiles across iOS, Android, shared music lookup, and production Firestore.
14. Production Firestore `artistPages/zweizwei-mave` and `artistPages/zweizwei-thadude` were updated with the exact Instagram and Spotify links provided for MAVE and ThaDude.
15. Production real-data guard test passed via `node --test functions/tests/production-real-data-guard.test.js`.
16. Android debug Kotlin compile passed via `./gradlew :androidApp:compileDebugKotlin`.
17. Android debug instrumentation Kotlin compile passed via `./gradlew :androidApp:compileDebugAndroidTestKotlin`.
18. iOS Debug simulator build passed via `xcodebuild -project 'Skydown App.xcodeproj' -scheme 'Skydown App' -configuration Debug -destination 'generic/platform=iOS Simulator' -derivedDataPath build/XcodeBuild-ArtistRouting build`.
19. Android `versionCode` bumped to `10029` and iOS build number bumped to `10026` for a device-visible artist page routing rollout.
20. Release identity preflight passed for iOS `SkyOS 1.0.0 (10026)` and Android `SkyOS 1.0.0 (10029)`.
21. Android clean release gate passed for version `1.0.0` / versionCode `10029`; AAB and APK were rebuilt at 2026-05-06 08:35 CEST.
22. Android AAB SHA-256 for versionCode `10029`: `977f97fb7b591fb22ba273b37884da6289684ec940d4d383f1c1d91f9f516eed`.
23. Android APK SHA-256 for versionCode `10029`: `491ae8587f419cbeadb125544c54a5109d31be131e0590cd25c9749f22056bf1`.
24. Fastlane `validate_android_internal` passed against Google Play for versionCode `10029` at 2026-05-06 08:58 CEST.
25. iOS archive build `10026` succeeded at `build/ios/SkyOS-1.0.0-10026-20260506.xcarchive`, and release identity re-check passed against the archive.
26. iOS build `10026` uploaded to App Store Connect successfully at 2026-05-06 08:43 CEST via `xcodebuild -exportArchive`; uploaded package is processing. Xcode reported missing vendor dSYMs for Firebase/gRPC binary frameworks, but the app package upload still succeeded.
27. Fastlane `upload_android_internal` uploaded versionCode `10029` to Google Play internal testing as a draft at 2026-05-06 09:00 CEST.

## 2026-05-05 Verification Log

1. Android `versionCode` bumped to `10024` for the Music Hub / localization follow-up.
2. Music Hub canonical artist order is aligned across iOS and Android: `Janno`, `Mave`, `Tangajoe007`, `Yang D. Nash`, `ThaDude`.
3. The duplicate top `22 Music`/Zweizwei social destination was removed, so the Music Hub shows only the five canonical artist entries.
4. Android release resources now include all configured app locales (`en`, `de`, `es`, `fr`, `it`, `ja`, `nl`, `pl`, `pt`, `tr`) instead of packaging English only.
5. Android release compile passed via `./gradlew :androidApp:compileReleaseKotlin`.
6. iOS Release simulator build passed via `xcodebuild -project 'Skydown App.xcodeproj' -scheme 'Skydown App' -configuration Release -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`.
7. Android clean release gate passed for version `1.0.0` / versionCode `10024`; AAB and APK were rebuilt at 2026-05-05 03:30 CEST.
8. Android AAB SHA-256 for versionCode `10024`: `d08c9775212bf9a21b78d03c3e00574676ef5f2243d29e4fd37436257e07deca`.
9. Android APK SHA-256 for versionCode `10024`: `379f02b71841e5866bdb8dd4bc7f32cf3bfa7dc447e95f004fa3d6df8fbd8dd4`.
10. Fastlane `validate_android_internal` passed against Google Play for versionCode `10024` at 2026-05-05 03:48 CEST.
11. Fastlane `upload_android_internal` uploaded versionCode `10024` to Google Play internal testing as a draft at 2026-05-05 03:50 CEST.
12. iOS build number bumped to `10023`; archive succeeded at `build/ios/SkyOS-1.0.0-10023-20260505.xcarchive`.
13. iOS build `10023` uploaded to App Store Connect successfully at 2026-05-05 03:54 CEST via `xcodebuild -exportArchive`; uploaded package is processing. Xcode reported missing vendor dSYMs for Firebase/gRPC binary frameworks, but the app package upload still succeeded.
14. Android `versionCode` bumped to `10025` so already-installed `10024` devices receive a real Play/App update.
15. Stale Android `music_instagram_hub_*` resources for the removed `22 Music` entry were deleted; release APK/AAB string scan found no `22 Music`, `22 Musik`, or `@zweizwei_music` remnants.
16. Android clean release gate passed for version `1.0.0` / versionCode `10025`; AAB and APK were rebuilt at 2026-05-05 04:23 CEST.
17. Android AAB SHA-256 for versionCode `10025`: `0d42b3ed244334896f4044f9b6231624e41a0781aa1364ae6d502a3dec50bd46`.
18. Android APK SHA-256 for versionCode `10025`: `9ccba604bb04a404eeeea0d6447f4843c106a78d6691e1fc8ce2c9c81ada1ada`.
19. Fastlane `validate_android_internal` passed against Google Play for versionCode `10025` at 2026-05-05 04:25 CEST.
20. Fastlane `upload_android_internal` uploaded versionCode `10025` to Google Play internal testing as a draft at 2026-05-05 04:27 CEST.
21. Shopify `syncShopifyMerch` and `listShopifyCollections` were redeployed with `SHOPIFY_ADMIN_ACCESS_TOKEN` attached as a Firebase secret dependency.
22. Functions/rules test suite passed locally on 2026-05-05 (`npm --prefix functions run test`, 82/82 passing).
23. Meta OAuth config in `adminConfig/metaOAuth` was refreshed with the current Graph token; server-side checks passed for Instagram Business Discovery, connected Instagram Business account, and Facebook Page.
24. Legal/Privacy/Terms last-updated labels were aligned to `5. Mai 2026` across iOS, Android, Functions defaults, and static web pages.
25. Production Firestore `appConfig/legalContent` was set to `lastUpdatedLabel = 5. Mai 2026` and verified live in project `skydown-a6add`.
26. iOS build number bumped to `10024`; Release simulator build passed and archive succeeded at `build/ios/SkyOS-1.0.0-10024-20260505.xcarchive`.
27. Release identity preflight passed for iOS `SkyOS 1.0.0 (10024)` and Android `SkyOS 1.0.0 (10026)`.
28. Android clean release gate passed for version `1.0.0` / versionCode `10026`; AAB and APK were rebuilt at 2026-05-05 18:30 CEST.
29. Android AAB SHA-256 for versionCode `10026`: `c0ae42bb63272c07e85458bc95fc0e6dfec954b445c290e3d03aa60705f44dee`.
30. Android APK SHA-256 for versionCode `10026`: `b39b96964db2c765a99593233bda7414ea2fa2c4811927cd4a1f8f07fec2a8b6`.
31. Fastlane `validate_android_internal` passed against Google Play for versionCode `10026` at 2026-05-05 18:32 CEST.
32. Fastlane `upload_android_internal` uploaded versionCode `10026` to Google Play internal testing as a draft at 2026-05-05 18:33 CEST.
33. iOS build `10024` uploaded to App Store Connect successfully at 2026-05-05 18:57 CEST via `xcodebuild -exportArchive`; uploaded package is processing. Xcode reported missing vendor dSYMs for Firebase/gRPC binary frameworks, but the app package upload still succeeded.
34. AI generated visuals can now be opened full-screen on Android and iOS; Android preview/save UX and iOS full-screen cover/share-save UX were added.
35. Android `versionCode` bumped to `10027` and iOS build number bumped to `10025` for the AI visual fullscreen reroll.
36. Release identity preflight passed for iOS `SkyOS 1.0.0 (10025)` and Android `SkyOS 1.0.0 (10027)`.
37. Android clean release gate passed for version `1.0.0` / versionCode `10027`; AAB and APK were rebuilt at 2026-05-05 23:29 CEST.
38. Android AAB SHA-256 for versionCode `10027`: `258ba35b312a9e787b6efbbf47ea1ab472cca7d8bce48e00e8574711945af742`.
39. Android APK SHA-256 for versionCode `10027`: `67fff098fa41c2839ad950dff94d4da948f872beb67e2b178c82ced7ae8c941a`.
40. Fastlane `validate_android_internal` passed against Google Play for versionCode `10027` at 2026-05-05 23:34 CEST.
41. Fastlane `upload_android_internal` uploaded versionCode `10027` to Google Play internal testing as a draft at 2026-05-05 23:37 CEST.
42. iOS archive build `10025` succeeded at `build/ios/SkyOS-1.0.0-10025-20260505.xcarchive`, and release identity re-check passed against the archive.
43. iOS build `10025` uploaded to App Store Connect successfully at 2026-05-05 23:40 CEST via `xcodebuild -exportArchive`; uploaded package is processing. Xcode reported missing vendor dSYMs for Firebase/gRPC binary frameworks, but the app package upload still succeeded.

## 2026-05-04 Verification Log

1. Release identity bumped for the latest prompt-progress rollout: iOS `SkyOS 1.0.0 (10022)` / `com.skydown.ios`; Android `SkyOS 1.0.0 (10023)` / `com.nash.skyos`.
2. Release identity preflight passed after archive creation: iOS archive path `build/ios/SkyOS-1.0.0-10022-20260504.xcarchive`; Android AAB/APK version matched `1.0.0 (10023)`.
3. Android clean release gate passed for version `1.0.0` / versionCode `10023`; AAB and APK were rebuilt at 2026-05-04 21:58 CEST.
4. Android AAB SHA-256 for versionCode `10023`: `a016d55546621e036dcc4cfff4263c07528246a1427b0dfd8218316e7edeaba0`.
5. Android APK SHA-256 for versionCode `10023`: `ffe39d9ee6a10f7b6a39551cfc8814c0226169540472c17c31ed7f9561073ed1`.
6. iOS archive build `10022` succeeded at `build/ios/SkyOS-1.0.0-10022-20260504.xcarchive`; archived Localizable strings include `Agent braucht Aufmerksamkeit`, `Erneut versuchen`, and matching English fallback strings.
7. iOS build `10022` uploaded to App Store Connect successfully at 2026-05-04 23:01 CEST via `xcodebuild -exportArchive`; uploaded package is processing. Xcode reported missing vendor dSYMs for Firebase/gRPC binary frameworks, but the app package upload still succeeded.
8. Fastlane `validate_android_internal` passed against Google Play for versionCode `10023` at 2026-05-04 23:06 CEST.
9. Fastlane `upload_android_internal` uploaded versionCode `10023` to Google Play internal testing as a draft at 2026-05-04 23:10 CEST.
10. Backend state before client rollout: `skydownAgent` and Firestore rules were deployed after Meta/social setup; sanitized health check showed Instagram Business Discovery and Facebook Page Graph both live.
11. This rollout is required for device-visible UI/asset updates: app logo refresh, social analytics copy/badges, Spotify public metadata fallback wording, Shopify collection cleanup, AI/Founder Briefing real-data guards, and the new AI/Agent prompt-progress indicators.

## 2026-04-30 Verification Log

1. Release identity preflight passed: iOS `SkyOS 1.0.0 (10020)` / `com.skydown.ios`; Android `SkyOS 1.0.0 (10021)` / `com.nash.skyos`.
2. Android clean release gate passed for version `1.0.0` / versionCode `10021`; AAB and APK were rebuilt at 2026-04-30 03:31 CEST.
3. Android AAB SHA-256 for versionCode `10021`: `321acb96607b2a8ba1803c1e185cab96da7f519fd858221a8c9b6fb31cd5ed4c`.
4. Android APK SHA-256 for versionCode `10021`: `25325466a90c0d471e9fa17ca874cd15c2dc93348f54ad1e9fdc16abe254316a`.
5. Fastlane `validate_android_internal` passed against Google Play for versionCode `10021` at 2026-04-30 03:33 CEST.
6. Fastlane `upload_android_internal` uploaded versionCode `10021` to Google Play internal testing as a draft at 2026-04-30 03:35 CEST.
7. iOS archive build `10020` succeeded at `build/ios/SkyOS-1.0.0-10020-20260430.xcarchive`.
8. iOS build `10020` uploaded to App Store Connect successfully at 2026-04-30 03:35 CEST via `xcodebuild -exportArchive`; uploaded package is processing.
9. This refreshed binary includes the post-`10019` Agent prompt UX: new prompted runs start a new conversation, follow-up input continues the current thread, and keyboard dismissal is wired across the Agent surfaces.

## 2026-04-29 Verification Log

1. Release identity preflight passed: iOS `SkyOS 1.0.0 (10019)` / `com.skydown.ios`; Android `SkyOS 1.0.0 (10020)` / `com.nash.skyos`.
2. Android clean release gate passed for version `1.0.0` / versionCode `10020`; AAB and APK were rebuilt at 2026-04-29 22:56 CEST.
3. Android AAB SHA-256 for versionCode `10020`: `824315ec528060a8fa50ac300e1dabc0cc572c3991567f238f2efc67a67b0439`.
4. Android APK SHA-256 for versionCode `10020`: `5a69342abe396636895e875e43f14104ce8efb7867fc0e6dd641b5b5a3cdc5dd`.
5. Fastlane `validate_android_internal` passed against Google Play for versionCode `10020` at 2026-04-29 22:59 CEST.
6. Fastlane `upload_android_internal` uploaded versionCode `10020` to Google Play internal testing as a draft at 2026-04-29 23:01 CEST.
7. iOS archive build `10019` succeeded at `build/ios/SkyOS-1.0.0-10019-20260429.xcarchive`.
8. iOS build `10019` uploaded to App Store Connect successfully at 2026-04-29 23:02 CEST via `xcodebuild -exportArchive`; uploaded package is processing.

## 2026-04-28 Verification Log

1. Android clean release gate passed for version `1.0.0` / versionCode `10018` and verified with `aapt2` from local Android SDK.
2. Android AAB SHA-256: `937ec888da21cf842100e5a77b87c5d4324674493c7bf5931b69f12495f1c5ae`.
3. Android APK SHA-256: `e17573dd72b9731ec87a373d4f788d49ddb0e88c7dc83c2fafc1d7b766afa32b`.
4. Firebase Secret Manager check confirmed the required Cloud Functions secrets exist in `skydown-a6add`: `SMTP_CONNECTION_URL`, `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `SHOPIFY_ADMIN_ACCESS_TOKEN`, `MANUS_API_KEY`, `XAI_API_KEY`, `AGENT_RUN_CALLBACK_SECRET`, and `SKYOS_WORKFLOW_SECRET`.
5. Release identity preflight passed after aligning this runbook with current source identity: iOS `SkyOS 1.0.0 (10018)` / `com.skydown.ios`; Android `SkyOS 1.0.0 (10018)` / `com.nash.skyos`.
6. Fastlane `validate_android_internal` passed against Google Play for versionCode `10018`.
7. Fastlane `upload_android_internal` uploaded versionCode `10018` to Google Play internal testing as a draft.
8. Fresh iOS UI-test run passed on a newly created iPhone 17 simulator (`B8B0F386-8D3D-4390-81B6-2784B9E2FB20`, iOS 26.4.1 / 23E254a): `xcodebuild test -project "Skydown App.xcodeproj" -scheme "Skydown App" -configuration Debug -destination "id=B8B0F386-8D3D-4390-81B6-2784B9E2FB20" -only-testing:"Skydown AppUITests" -resultBundlePath "build/ios/Skydown-App-10018-ui-tests-fresh-20260428-170034.xcresult" -derivedDataPath "build/DerivedData/Skydown-App-10018-ui-tests-fresh-20260428-170034" CODE_SIGNING_ALLOWED=NO`.
9. iOS UI-test result: PASS, 16 total tests, 12 passed, 4 intentionally skipped due missing `SKYOS_RUN_LIVE_*` App-Check flags, 0 failures; raw log: `build/ios/Skydown-App-10018-ui-tests-fresh-20260428-170034.log`.
10. Relevant UI-test tooling note: repeated `DebuggerLLDB.DebuggerVersionStore.StoreError error 0` / `no debugger version` warnings appeared during launches, but the run completed without hang and ended with `** TEST SUCCEEDED **`.
11. Release identity preflight passed after the fresh UI-test run: iOS `SkyOS 1.0.0 (10018)` / `com.skydown.ios`; Android `SkyOS 1.0.0 (10018)` / `com.nash.skyos`.
12. iOS build `10018` uploaded to App Store Connect successfully at 2026-04-28 17:17 CEST via `xcodebuild -exportArchive`; archive metadata records `uploadedBuildNumber` `10018`, upload event state `success`, and uploaded package processing started.
13. Android local release artifact verify passed after iOS upload; current local hashes: AAB `f84be7d63da70b92c458b4a264a0700825a14693eb677d45c4d3e40af48173c4`, APK `38531fd872dfb5cd7291f6f7f99332eae163ad3c71ee9ad7782fccc4e6ed979c`.
14. Android `versionCode` was bumped from `10018` to `10019` for a fresh post-17:00 Play upload.
15. Android clean release gate passed for version `1.0.0` / versionCode `10019`; artifacts were rebuilt at 2026-04-28 17:32 CEST.
16. Android AAB SHA-256 for versionCode `10019`: `af61d25c91edcfa91db8dd2e8bfaeedd13ae32d051e853ed3c13daee0062ff83`.
17. Android APK SHA-256 for versionCode `10019`: `865dc4790c7e7073f9c5f44ae3f19e7640da7c18e02a20a54d5848a35cc74772`.
18. Fastlane `validate_android_internal` passed against Google Play for versionCode `10019` at 2026-04-28 17:33 CEST; log: `build/android-10019-validate-20260428-1733.log`.
19. Fastlane `upload_android_internal` uploaded versionCode `10019` to Google Play internal testing as a draft at 2026-04-28 17:34 CEST; log: `build/android-10019-upload-20260428-1734.log`.
20. Final release identity preflight passed: iOS `SkyOS 1.0.0 (10018)` / `com.skydown.ios`; Android `SkyOS 1.0.0 (10019)` / `com.nash.skyos`.

## 2026-04-27 Verification Log

1. Android clean release gate passed for version `1.0.0` / versionCode `10016` and verified with `aapt2` from local Android SDK.
2. Android AAB SHA-256: `99e4d4840005e3d0eb686937f53224e6198f72a63e33f81ef5f861889f0d74f3`.
3. Android APK SHA-256: `a058b9d56e4814bfa521e16205effc399035e5ebf6caa5e837103ab945fd94c9`.
4. iOS Release build passed with code signing disabled for compile validation.
5. iOS Debug simulator build passed after the Music Studio split fix and Agent tap hardening.
6. iOS UI screenshot test passed through the Agent tap path and waited for `agent.screen.root`.
7. iOS build `10010` archived at `build/ios/SkyOS-1.0.0-10010-20260427.xcarchive`; archive identity verified as `SkyOS` / `com.skydown.ios` / `1.0.0` / `10010`.
8. iOS build `10010` uploaded to App Store Connect successfully at 2026-04-27 23:48 CEST; uploaded package is processing.
9. Previous iOS build `10008` uploaded to App Store Connect successfully; build `10009` was superseded before successful upload; that pass used build `10010`, now superseded by current build `10029`.
10. Local CI gate passed: shared tests, Android lint, Functions tests, Firestore rules tests, and Storage rules tests.
11. Detekt passed with `./gradlew detektAll --no-daemon`.
12. Release identity preflight passed: iOS `SkyOS 1.0.0 (10010)` / `com.skydown.ios`; Android `SkyOS 1.0.0 (10016)` / `com.nash.skyos`.
13. Store screenshot folders contain 7-frame iPad/phone/fold/iPhone sets. Play-compliant Android phone exports were added at `screenshots/final/google-play/android-phone/` (`1242x2424`, no alpha), iPad screenshots were exported at `screenshots/final/ipad/` (`2064x2752`, no alpha), and Google Play listing graphics were exported under `docs/assets/google-play/`.

## Execution Log (Direct Upload Run)

Historical 2026-04-26 upload notes retained for traceability; current upload targets are listed above.

1. Deployed AI callable fix to Firebase Functions (`authorizeAiUsage`, `generateAiText`, `generateAiVisual`, `skydownAgent`) -> deploy complete.
2. Rebuilt Android release bundle (`./gradlew :androidApp:bundleRelease`) -> `BUILD SUCCESSFUL` for versionCode `10014`.
3. Rebuilt iOS archive (`xcodebuild archive`) -> `ARCHIVE SUCCEEDED` for build `10006`.
4. Exported iOS IPA (`xcodebuild -exportArchive` with `destination=export`) -> `EXPORT SUCCEEDED`.
5. Uploaded iOS build `10006` to App Store Connect (`xcodebuild -exportArchive` with `destination=upload`) -> `EXPORT SUCCEEDED`; uploaded package is processing.
6. Play upload via CLI was not rerun because `SUPPLY_JSON_KEY` is still unset and no service-account JSON was found locally; upload the AAB manually or rerun Fastlane after exporting the service-account JSON path.

## Post-Upload Hardening Notes

- iOS app privacy manifest added at `Skydown App/PrivacyInfo.xcprivacy` for first-party `UserDefaults` usage.
- Android Spotify OAuth token, refresh token, and PKCE verifier storage hardened with Android Keystore-backed AES-GCM encryption and legacy plaintext preference migration.
- Android native Play Billing purchase flow now waits for the Play Billing purchase callback before treating the subscription as complete or cancelled.
- Android Play Billing Library upgraded from `7.1.1` to `8.3.0`; `queryProductDetailsAsync` migrated to the PBL 8 `QueryProductDetailsResult.productDetailsList` API.
- Android custom-scheme entry points are limited to Spotify auth and checkout return hosts.
- Android UI-test launch extras are ignored in non-debuggable builds, so release builds cannot be started into mock data or a local fixture user through exported Activity extras.
- Android release manifest explicitly removes SDK-injected advertising ID / AdServices permissions.
- Public privacy/terms pages no longer expose beta placeholders or "not final before release" wording.
- iOS and Android in-app legal defaults now use the same operator/contact baseline as the public legal pages.
- App Check defaults are set to enforce in code; verify production runtime config does not override them back to monitor/soft-fail.
- Public Video Hub now opens a focused player/reel instead of showing a scrollable public video list; owner-only video ordering, Home feature control, and deletion remain in the admin/premium control sheet.
- Android Music Hub uses a real lazy scroll surface so new sections can grow without blocking the page.
- AI Bot/Agent surfaces use the FAB-driven prompt sheet; the persistent inline input is removed from the AI chat screen.
- AI Bot/Agent chat surfaces now sit directly on the shared SkyOS atmosphere background instead of a separate gradient panel.
- Server-side AI authorization now requires an active Pro/Creator entitlement for non-staff users before provider calls are released; owner/admin accounts remain staff-gated with internal limits.
- Android Google Sign-In was smoke-tested on the Pixel 9 emulator after adding the local debug OAuth SHA1 client to `google-services.json`.
- App Check callable flow was smoke-tested after registering the current Android Studio emulator debug token; `generateAiText` returned a real response and Cloud Logging showed `AI usage authorized`.

## Backend Deployment Log

- Firebase project binding added in `.firebaserc`: `skydown-a6add`.
- Dry-run passed on 2026-04-25:
  - `npx firebase-tools deploy --dry-run --non-interactive --project skydown-a6add --only firestore:rules,storage,functions`
- Live deploy passed on 2026-04-25:
  - `npx firebase-tools deploy --non-interactive --project skydown-a6add --only firestore:rules,storage,functions`
- Firestore index deploy passed on 2026-04-25:
  - `npx firebase-tools deploy --non-interactive --project skydown-a6add --only firestore:indexes`
- Deployed surfaces:
  - Cloud Firestore rules
  - Cloud Firestore composite indexes for AI FAQ review revert, denied AI usage reporting, and featured public video lookup
  - Firebase Storage rules
  - Cloud Functions, including account deletion purge hardening, App Check callable gate updates, AI membership/admin functions, and `validateManusApiKey` auth protection.
- Firestore rules cleanup:
  - Removed unsupported `rules.List.all(...)` usage from `artistPages.studioPriceList` validation.
  - `studioPriceList` is now bounded to 12 list entries in Rules; app-side readers still only materialize valid `{title, detail, price}` entries.
- Owner Auth fallback check:
  - Firebase Auth export check on 2026-04-25 found the fixed owner account and confirmed `emailVerified=true`.
- AI callable hotfix:
  - Deployed on 2026-04-26 to `authorizeAiUsage`, `generateAiText`, `generateAiVisual`, and `skydownAgent`.
  - Fixed invalid Firestore event payloads when canonical AI entitlement fields such as provider/productId are absent.
  - Post-deploy Android emulator smoke test returned a real SkyOS AI response instead of `INTERNAL`.
- AI subscription gate:
  - Deployed on 2026-04-26 to `authorizeAiUsage`, `generateAiText`, `generateAiVisual`, and `skydownAgent`.
  - Non-staff users now need an active Pro/Creator entitlement before AI provider calls are authorized.
  - Owner/admin access remains staff-gated with internal routing and limits.

## Metadata and URLs to Fill In Console

Use **HTTPS URLs** hosted from `site/` (Firebase Hosting):

- Firebase default host: **`https://skydown-a6add.web.app`**
  - Privacy: `https://skydown-a6add.web.app/privacy.html`
  - Terms: `https://skydown-a6add.web.app/terms.html`
  - Support: `https://skydown-a6add.web.app/support.html`
  - Home: `https://skydown-a6add.web.app/`

If you map a custom domain in Firebase Hosting, swap the hostname but keep **`/privacy.html`**, **`/terms.html`**, **`/support.html`** (or update redirects alongside `firebase.json`).

Do not submit with undeployed domains or placeholders.

## App Review Notes (Apple)

Suggested review note text:

`SkyOS is the public product name. Internal package and repository naming may still contain Skydown identifiers.`

`Test account (if required): <email> / <password-or-test-flow>.`

`Core user flow for review: Launch app -> sign in/sign up -> access AI workspace, media tabs, and membership/settings surfaces.`

`If subscription products are attached, use Apple sandbox test users for purchase/restore verification.`

## Release Notes

### iOS "What to Test" / TestFlight notes
- NICMA MUSIC and NICMA STUDIO now open as separate iOS Music pages, matching Android.
- Agent mode startup is hardened so tapping Agent opens the surface before live task/note observers attach.
- Unified first-session growth tracking events across startup, onboarding, signup, and first value.
- Stability and trust cleanup across launch and public-facing support/legal surfaces.
- Video Hub player flow replaces the public video list; owner controls stay admin-only.
- General reliability and navigation polish.

### Google Play release notes
- Unified first-session growth tracking events across startup, onboarding, signup, and first value.
- Stability and trust cleanup across launch and public-facing support/legal surfaces.
- Video Hub player flow replaces the public video list; owner controls stay admin-only.
- General reliability and navigation polish.

## Post-Release Monitoring

Run these checks from the shipped store builds, not from local debug builds:

1. Install/update iOS build `10030` from the store/TestFlight channel used for release and complete a real-device smoke pass.
2. Install/update Android versionCode `10033` from Play internal/closed/public channel used for release and complete a real-device smoke pass.
3. Watch Crashlytics for new fatal/non-fatal clusters for the first 2 hours, then again after 24 hours.
4. Verify Analytics/Firebase events for app open, onboarding, signup/login, AI use, membership views, purchase/restore outcomes, and first value.
5. Verify one successful auth path, one AI text path, one Music path, one Video path, one Shop/Orders path, Settings legal/support, notification permission, and one reminder notification.
6. Confirm StoreKit/Google Play subscription metadata, pricing, entitlement unlock, and restore behavior from sandbox/license-test users before widening rollout.
7. Record any console review messages or policy warnings here before increasing distribution.

### 2026-05-12 Device Smoke Attempt

- Android basic smoke: PASS on device `SM_F946B` / `RFCW70AHR6X`. Installed package `com.nash.skyos` reports `versionName=1.0.0`, `versionCode=10033`, `targetSdk=36`, installer `com.android.vending`, and `lastUpdateTime=2026-05-12 13:20:09`. Launch succeeded, `com.nash.skyos/.MainActivity` stayed foreground, UI dump showed `SkyOS` / `WILLKOMMEN`, notification app-op is `allow`, and captured Logcat had no `FATAL EXCEPTION` / `AndroidRuntime` crash lines after launch.
- Android expanded navigation smoke: PASS on the Play-installed `10033` artifact. Checked Music, Video, Merch, KI, More menu, Settings, Datenschutz/Privacy content, Orders, and Payments. Each surface rendered expected user-visible labels and the app process stayed alive with no app-specific Logcat fatal/crash lines during the checks.
- Android post-release finding: the released German Android Payments screen renders `Stripe` as `Streifen` in the provider title. This is not a crash or store-blocking policy issue, but it is a brand/copy defect. Local source was corrected after release in `androidApp/src/main/res/values-de/strings.xml`; before uploading a hotfix, bump Android `versionCode` above `10033`, rebuild, and upload a new Play artifact.
- Android post-release text hotfix validation: `./gradlew :androidApp:lintRelease` passed after correcting German `Stripe`, `common_close`, and `order_action_close` strings.
- Android hotfix upload: PASS for versionCode `10034`. `./scripts/release_identity_check.sh` and `./scripts/android_release_gate.sh` passed; AAB SHA-256 is `ba9adc71291d734bbc12b13114a953b30c2b99bf5b0e3002ec7932eacd65dd92`, APK SHA-256 is `0d7a2ee20b2ff6f1013aa0aecd3cd3197aa7cda727865204579f75568429c40b`. AAPT resource dump confirmed German `settings_payment_method_stripe = Stripe`, `common_close = Schließen`, and `order_action_close = Schließen`. Google Play validation passed and versionCode `10034` was uploaded to internal testing as a draft at 2026-05-12 14:28 CEST; no production rollout was started. A later API check confirmed `10034` completed on the internal track, with no production/beta/alpha release.
- Android Play Store availability check: at 2026-05-12 15:02 CEST, Samsung Fold `SM_F946B` / `RFCW70AHR6X` was connected by USB. Play Store opened `market://details?id=com.nash.skyos` and showed `com.nash.skyos (unreviewed)` with buttons `Deinstallieren` and `Öffnen`, but no `Aktualisieren`. Installed package still reports Play-installed `versionCode=10033`, so `10034` is not tester-available yet.
- Android Play Store recheck: at 2026-05-12 15:15 CEST, Google Play API had advanced internal `10034` to `completed`, but Android Package Manager on the Samsung Fold still reported `package:com.nash.skyos versionCode:10033`, `installerPackageName=com.android.vending`, `lastUpdateTime=2026-05-12 15:15:04`. The Play Store page still showed `Öffnen`, not `Aktualisieren`, through 15:20 CEST. Local release APK badging confirmed the candidate artifact itself was `versionCode=10034`.
- Android Play-installed hotfix smoke: PASS at 2026-05-12 17:17 CEST on Samsung Fold `SM_F946B` / `RFCW70AHR6X`. Android Package Manager reports `com.nash.skyos` `versionCode=10034`, `versionName=1.0.0`, `installerPackageName=com.android.vending`, `lastUpdateTime=2026-05-12 17:03:47`. The installed base APK badging reports `versionCode='10034'`, and the installed German split confirms `settings_payment_method_stripe = Stripe`, `common_close = Schließen`, and `order_action_close = Schließen`. Smoke covered launch/main shell, Merch, Musik, Video (screenshot evidence `build/android-10034-video-final.png` because UIAutomator idle detection is blocked by the active video surface), KI account gate, Settings, Datenschutz, and Orders. Recent Logcat scan had no `FATAL EXCEPTION` / app crash signature.
- iOS basic smoke: PASS on `iPhone von Yang D. Nash` / `9FFBA1FD-0734-5E66-9A33-A07D00BF5A54`. Device is paired, connected over USB, Developer Mode is enabled, and lock state reports `unlockedSinceBoot=true`. Installed app list reports `SkyOS` / `com.skydown.ios` / `1.0.0` / build `10030`. Launch of `com.skydown.ios` succeeded, and the process list showed `/Skydown App.app/Skydown App` running after launch (confirmed again at 2026-05-12 13:51 CEST with PID `682`).
- iOS expanded store-build navigation smoke: PASS at 2026-05-12 14:12 CEST using a destination-artifacts XCUITest runner against the already-installed `com.skydown.ios` store/TestFlight artifact. Result bundle: `build/ios/StoreSmokeDerived/Results-store-installed-navigation.xcresult`. Summary: iPhone 16 Pro / iOS `26.4.2`, 1 test executed, 1 passed, 0 failed. Checked launch/main shell, Merch, Music, Video, AI, Settings, and Privacy/Datenschutz visibility, with kept screenshots `store-smoke-01-shell` through `store-smoke-06-settings-legal`. The temporary `com.skydown.iosUITests.xctrunner` app was removed afterward, and `SkyOS` remained installed as `1.0.0 (10030)`.
- iOS note: `iPhone von Darinka` still has the superseded `SkyOS 1.0.0 (10029)` installed and should not be used as release-build evidence until updated to build `10030`.

## Manual Remaining Tasks

1. Confirm final legal approval for public privacy/terms wording.
2. Confirm final production domain and replace URL placeholders in App Store Connect and Play Console.
3. Verify subscription product setup status for iOS build `10030` and Android versionCode `10034` in the tester channel before widening Android beyond internal testing.
4. Update production Firestore `appConfig/legalContent` and `appConfig/commerceSettings` if old remote operator/legal values still exist.
5. Firestore/Storage rules were deployed on 2026-04-25; fixed owner Firebase Auth account was verified with `emailVerified=true`.
6. Verify data safety/privacy forms reflect actual SDK usage:
   - Auth/account: email, user ID/login state, account identifiers.
   - App activity/analytics: app open, onboarding, signup, membership views, plan selection, purchase/restore outcomes. These are linked to the signed-in UID in `recordAiMembershipEvent`; Android also logs selected events through Firebase Analytics.
   - Purchases/subscriptions: StoreKit/Google Play product IDs, transaction or purchase references, entitlement/restore status.
   - User content: profile data, uploads, AI prompts/outputs, media/gallery selections and support/workflow requests where a user submits them.
   - Device/app data: app version, platform, security/App Check/abuse signals, Firebase installation or app instance identifiers. Advertising ID and AdServices permissions are intentionally removed from Android release manifests.
   - Not used by current binaries: precise/coarse location, camera capture, microphone, contacts, calendar. Photo/video selection uses system pickers; Android `WRITE_EXTERNAL_STORAGE` is capped to API 28 only for saving generated images.
7. Upload and map final screenshot sets for iPhone, iPad, and Android phone form factors. Use `screenshots/final/ipad/` for iPad, `screenshots/final/google-play/android-phone/` for Play phone screenshots, and `docs/assets/google-play/` for the Play icon/feature graphic.
8. Set age rating/content rating questionnaires in both consoles.
9. Build numbers are current for the post-hardening release plus Android copy hotfix: iOS build `10030` and Android `10034` are the active project/upload candidates. The currently confirmed released Android build remains `10033` until `10034` is promoted. Previous uploads `10029`/`10032` are superseded.

Use `docs/release/store-console-submission-worksheet.md` while filling App Store Connect and Google
Play Console. It contains reviewer notes, Data Safety/App Privacy draft categories, Exact Alarm
status, public URL values, and the localization language decision for this RC.

### 2026-05-12 Local Release Audit Addendum

- Release owner confirmed the app is out at 2026-05-12 13:24 CEST; post-release monitoring is active.
- Localization guard passed after moving the reported Settings literals into localized resources.
- iOS `Localizable.strings` files validate with `plutil -lint`.
- Release identity check passed after the build identity bump to iOS `SkyOS 1.0.0 (10030)` / `com.skydown.ios` and Android `SkyOS 1.0.0 (10033)` / `com.nash.skyos`, including the new local iOS archive.
- iOS App Store export passed without uploading; IPA SHA-256 is `53763455354820e7945abd3868f97dcd6fc2a89910843e3d9a96efd1a59d9b7f`.
- iOS App Store Connect upload succeeded for build `10030` at 2026-05-12 13:15 CEST; package processing started. Xcode still reported missing dSYMs for FirebaseFirestoreInternal, absl, grpc, grpcpp, and openssl_grpc, but the app package upload succeeded.
- Store screenshot audit passed.
- Fresh Android release gate passed for versionCode `10033`; AAB SHA-256 is `231b6c5819e1ecb05822a852fefde98695d9855b30ac40ab679960d04bd112f7`, APK SHA-256 is `73ea590606f1ca093dfa30ba87e59da092e60a2d4c7dffb9fedbb4602587dfb7`.
- Google Play `validate_android_internal` passed and `upload_android_internal` uploaded versionCode `10033` as an internal-testing draft at 2026-05-12 13:12 CEST. This did not start a production rollout.
- Android hotfix versionCode `10034` passed release identity, clean release gate, Google Play validation, internal draft upload at 2026-05-12 14:28 CEST, internal track completion at 2026-05-12 15:15 CEST, and Play-installed Samsung Fold smoke at 2026-05-12 17:17 CEST. This did not start a production rollout.
- Final Android APK permission dump confirms `targetSdkVersion 36` and no `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` permission.
- Full local CI gate passed after the build identity bump: shared tests, Android lint/detekt, Functions syntax build, 36 Node tests, and 86 Firestore/Storage rules tests. The run still warns that local Node is `v24.13.1` while Functions declares Node `22`.
- Android exact-alarm release risk removed from source by dropping `SCHEDULE_EXACT_ALARM`; reminders now use the inexact `setAndAllowWhileIdle(...)` scheduling path.
- Cold-launch notification prompts removed; notification permission is now requested from contextual settings/notification flows, while iOS only registers at launch if permission already exists.
- Functions dependency lockfile was updated with non-breaking audit fixes (`firebase-admin` `13.9.0`, `fast-uri` `3.1.2`, `fast-xml-builder` `1.2.0`, `uuid` `11.1.1`, `@tootallnate/once` `2.0.1`). Full audit is reduced from 27 to 24 findings; production-only audit is reduced from 23 to 20 findings. Remaining production findings are no-fix transitive chains through Genkit / OpenTelemetry / Google Cloud and must be accepted or tracked separately before public production.
- Broader localization audit still reports legacy hardcoded UI literals. For the fastest RC, keep store metadata languages limited to reviewed languages, recommended English and German, or complete a full 10-language UI localization pass before public production.

## Go/No-Go Checklist

- [x] Release owner confirmed app is out
- [x] iOS build `10030` archived locally
- [x] iOS build `10030` exported locally as App Store IPA
- [x] iOS build `10030` uploaded to App Store Connect
- [x] Android release `10033` built and verified locally
- [x] Android release `10033` uploaded to Play Console internal testing as a draft
- [x] Android hotfix `10034` built, verified, validated, and uploaded to Play Console internal testing as a draft
- [x] Android hotfix `10034` Play API status verified as internal `completed`; no production/beta/alpha rollout exists
- [x] Android hotfix `10034` Play internal track completed and Play-installed tester smoke passed
- [x] Basic post-release launch smoke passed on Android `10033` and iOS `10030`
- [x] Android post-release functional navigation smoke passed from the Play-installed `10033` artifact
- [x] iOS post-release functional navigation smoke passed from the installed store/TestFlight `10030` artifact
- [ ] Privacy, terms, support URLs point to final public domain
- [ ] Legal text approved for store/public use
- [ ] Subscription metadata, pricing, and restore behavior validated
- [ ] Data safety/privacy questionnaire completed and consistent with app behavior
- [ ] Content/Age rating completed
- [ ] Screenshots and listing metadata complete in target languages, including uploaded iPad and Google Play assets
- [x] Play Console review/submission completed so Android hotfix `10034` leaves draft state
- [x] Android hotfix `10034` Play-installed tester smoke passed after it becomes available

## Exact Next Clicks: App Store Connect

1. Open [App Store Connect](https://appstoreconnect.apple.com/).
2. Go to **My Apps** -> select/create app for bundle `com.skydown.ios`.
3. Open **TestFlight** tab.
4. Confirm build `10030` is the active released/tester build and record its current status.
5. Review any processing/review/policy warnings for build `10030`.
6. Confirm internal tester or public availability matches the intended release scope.
7. Fill **App Information** and **App Privacy** sections.
8. Fill **Pricing and Availability** (manual business decision required).
9. Fill **App Review Information** with support contact + reviewer login instructions (Google sign-in; no seeded release test account).
10. Keep production/public changes aligned with the confirmed release scope; do not widen rollout without explicit approval.

## Exact Next Clicks: Google Play Console

1. Open [Google Play Console](https://play.google.com/console/).
2. Select app with package `com.nash.skyos`.
3. Go to **Dashboard** and resolve any remaining setup tasks that block wider rollout.
4. Go to **Store presence** and complete store listing fields.
5. Go to **App content** and complete:
   - Privacy policy URL
   - Data safety
   - Ads declaration (if applicable)
   - Content rating questionnaire
6. Go to **Pricing and availability** and select target production countries/regions. API read currently returns empty production country availability.
7. Go to **Monetize** for in-app products/subscriptions validation.
8. Go to **Release** -> **Production**. If **Create new release** is disabled, follow the listed Play Console blocker.
9. Use versionCode `10034` from the app bundle library, review release notes, and continue to **Preview and confirm**.
10. If the page shows **Errors summary**, expand it and resolve the required items before rollout.
11. If changes land in **Publishing overview**, send the ready changes for review there and record the resulting status.
12. Keep public changes aligned with the confirmed release scope; the API validation has not committed any production/closed release.

Google references for the console flow: [Prepare and roll out a release](https://support.google.com/googleplay/android-developer/answer/9859348/prepare-and-roll-out-a-release?hl=en-GB), [Set up an internal test](https://support.google.com/googleplay/android-developer/answer/9845334?hl=en), and [Publishing overview/review control](https://support.google.com/googleplay/android-developer/answer/9859654?hl=en).
