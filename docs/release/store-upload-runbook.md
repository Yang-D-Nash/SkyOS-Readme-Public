# SkyOS Store Upload Runbook

Last updated: 2026-04-27 (release readiness check: iOS 10008 prepared, Android 10015)
Owner: Release Engineering

## Build Identity

### iOS
- Bundle ID: `com.skydown.ios`
- Display Name: `SkyOS` (from `SkydownApp-Info.plist`)
- Version: `1.0.0`
- Build: `10008` (next upload; last archived build: `10007`)
- Team ID: `F3BNLG6L7P`

### Android
- Application ID: `com.nash.skyos`
- App Label: `SkyOS`
- versionName: `1.0.0`
- versionCode: `10015`
- Play Billing Library: `8.3.0`

## Build Artifacts

- iOS archive: `build/ios/SkyOS-1.0.0-10007-20260427.xcarchive` (rebuilt 2026-04-27 Europe/Berlin)
- iOS upload status: App Store Connect rejected a repeat upload for build `10007` as redundant, which indicates build `10007` already exists for version `1.0` / `1.0.0`.
- Android AAB: `androidApp/build/outputs/bundle/release/androidApp-release.aab` (rebuilt 2026-04-27 14:48 Europe/Berlin, versionCode `10015`)
- Android APK: `androidApp/build/outputs/apk/release/androidApp-release.apk` (rebuilt 2026-04-27 14:48 Europe/Berlin, versionCode `10015`)

## Upload Status

### iOS Upload Status
- Archive build: DONE for build `10007`
- App Store Connect upload: NOT rerun successfully during this check; the server reported redundant binary for build `10007`.
- Processing status: verify existing build `10007` in App Store Connect and attach it to internal TestFlight testers if available.
- Notes:
  - `CURRENT_PROJECT_VERSION` is now set to `10008`; generate a fresh archive before uploading again.
  - Build `10007` was archived successfully on 2026-04-27.
  - The redundant-upload response for build `10007` means reusing the same build number will not work.
  - Build `10006` uploaded successfully on 2026-04-26 via `xcodebuild -exportArchive` with `destination=upload`.
  - Build `10005` uploaded successfully on 2026-04-26 via `xcodebuild -exportArchive` with `destination=upload`.
  - Xcode reported missing dSYMs for Firebase binary frameworks during symbol upload; app package upload still succeeded.
  - Previous build `10005` was superseded by build `10006` after the AI callable entitlement fix and smoke test.
  - Previous build `10004` was superseded by build `10005` for the public Video Hub player cleanup.
  - Previous build `10003` had resolved the Apple AppIcon alpha rejection (`90717`) by converting active AppIcon PNGs to opaque RGB.

### Google Upload Status
- Release AAB build: DONE for versionCode `10015`
- Play upload automation: CONFIGURED via Fastlane `upload_android_internal`
- CLI validate-only attempt: BLOCKED by Google Cloud API configuration
- Blocker details:
  - Fastlane was run with `SUPPLY_JSON_KEY` pointing at a local Play service-account JSON and `validate_only: true`.
  - Google Play Android Developer API is disabled or not yet propagated for project `1069068117600`.
  - Enable `androidpublisher.googleapis.com` for project `1069068117600`, wait for propagation, then rerun the validate-only lane before any automated upload.

## 2026-04-27 Verification Log

1. Android clean release gate passed for version `1.0.0` / versionCode `10015` after gating UI-test launch extras to debuggable builds only.
2. Android AAB SHA-256: `5e03e53452aa1ae29ce5d13bc551bc641924a8d19cb1fe8ae2be5b2b98e05e97`.
3. Android APK SHA-256: `6aa428722645f0af697080fcb3c16443005dd7649426442969fc0d89e20c359d`.
4. iOS Release build passed with code signing disabled for compile validation.
5. iOS archive passed for `com.skydown.ios`, version `1.0.0`, build `10007`.
6. Local CI gate passed: shared tests, Android lint, Functions tests, Firestore rules tests, and Storage rules tests.
7. Detekt passed with `./gradlew detektAll --no-daemon`.
8. Store screenshot folders contain 7-frame iPad/phone/fold/iPhone sets. Play-compliant Android phone exports were added at `screenshots/final/google-play/android-phone/` (`1242x2424`, no alpha), iPad screenshots were exported at `screenshots/final/ipad/` (`2064x2752`, no alpha), and Google Play listing graphics were exported under `docs/assets/google-play/`.

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

- Privacy Policy URL: `<your-public-domain>/privacy.html`
- Terms URL: `<your-public-domain>/terms.html`
- Support URL: `<your-public-domain>/support.html`
- App website URL (if required): `<your-public-domain>/`

Do not submit with placeholder domains.

## App Review Notes (Apple)

Suggested review note text:

`SkyOS is the public product name. Internal package and repository naming may still contain Skydown identifiers.`

`Test account (if required): <email> / <password-or-test-flow>.`

`Core user flow for review: Launch app -> sign in/sign up -> access AI workspace, media tabs, and membership/settings surfaces.`

`If subscription products are attached, use Apple sandbox test users for purchase/restore verification.`

## Release Notes

### iOS "What to Test" / TestFlight notes
- Unified first-session growth tracking events across startup, onboarding, signup, and first value.
- Stability and trust cleanup across launch and public-facing support/legal surfaces.
- Video Hub player flow replaces the public video list; owner controls stay admin-only.
- General reliability and navigation polish.

### Google Play release notes
- Unified first-session growth tracking events across startup, onboarding, signup, and first value.
- Stability and trust cleanup across launch and public-facing support/legal surfaces.
- Video Hub player flow replaces the public video list; owner controls stay admin-only.
- General reliability and navigation polish.

## Manual Remaining Tasks

1. Confirm final legal approval for public privacy/terms wording.
2. Confirm final production domain and replace URL placeholders in App Store Connect and Play Console.
3. Verify subscription product setup status for iOS build `10007` and Android versionCode `10015`.
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
9. Build numbers are current for the 2026-04-27 check: iOS `10007`, Android `10015`.

## Go/No-Go Checklist

- [ ] Existing iOS build `10007` visible in App Store Connect TestFlight, or upload a newly archived build `10008`
- [ ] Android release visible in Play Console internal/closed track
- [ ] Privacy, terms, support URLs point to final public domain
- [ ] Legal text approved for store/public use
- [ ] Subscription metadata, pricing, and restore behavior validated
- [ ] Data safety/privacy questionnaire completed and consistent with app behavior
- [ ] Content/Age rating completed
- [ ] Screenshots and listing metadata complete in target languages, including uploaded iPad and Google Play assets
- [ ] Internal smoke test passed on real iOS and Android devices from uploaded store artifacts

## Exact Next Clicks: App Store Connect

1. Open [App Store Connect](https://appstoreconnect.apple.com/).
2. Go to **My Apps** -> select/create app for bundle `com.skydown.ios`.
3. Open **TestFlight** tab.
4. Under **Builds**, click **+** and look for existing build `10007`.
5. If build `10007` is available, attach it to Internal Testers. If it is not available and a new binary is required, increment the iOS build number to `10008` before uploading.
6. When build appears, assign to Internal Testers first.
7. Fill **App Information** and **App Privacy** sections.
8. Fill **Pricing and Availability** (manual business decision required).
9. Fill **App Review Information** with support contact + test account.
10. Save draft; do not submit for external/public review without explicit approval.

## Exact Next Clicks: Google Play Console

1. Open [Google Play Console](https://play.google.com/console/).
2. Select app with package `com.nash.skyos` (or create it if missing).
3. Go to **Testing** -> **Internal testing** (or **Closed testing**).
4. Create/Edit release and upload `androidApp/build/outputs/bundle/release/androidApp-release.aab` (versionCode `10015`).
5. Add release notes and save.
6. Go to **Store presence** and complete store listing fields.
7. Go to **App content** and complete:
   - Privacy policy URL
   - Data safety
   - Ads declaration (if applicable)
   - Content rating questionnaire
8. Go to **Monetize** for in-app products/subscriptions validation.
9. Roll out to internal/closed testers only; do not start production rollout without explicit approval.
