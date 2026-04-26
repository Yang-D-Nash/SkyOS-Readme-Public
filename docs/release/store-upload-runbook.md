# SkyOS Store Upload Runbook

Last updated: 2026-04-26 (Android 10014, AI subscription gate)
Owner: Release Engineering

## Build Identity

### iOS
- Bundle ID: `com.skydown.ios`
- Display Name: `SkyOS` (from `SkydownApp-Info.plist`)
- Version: `1.0.0`
- Build: `10006`
- Team ID: `F3BNLG6L7P`

### Android
- Application ID: `com.nash.skyos`
- App Label: `SkyOS`
- versionName: `1.0.0`
- versionCode: `10014`
- Play Billing Library: `8.3.0`

## Build Artifacts

- iOS archive: `build/ios/SkyOS-1.0.0-10006-20260426.xcarchive` (rebuilt 2026-04-26 Europe/Berlin)
- iOS IPA export: `build/ios/export-app-store/SkyOS.ipa` (App Store Connect export succeeded)
- iOS upload: direct App Store Connect upload from archive; build `10006` uploaded and is processing.
- Android AAB: `androidApp/build/outputs/bundle/release/androidApp-release.aab` (rebuilt 2026-04-26 Europe/Berlin, versionCode `10014`)
- Android APK: `androidApp/build/outputs/apk/release/androidApp-release.apk` (`18997697` bytes, rebuilt 2026-04-25 13:12:14 Europe/Berlin)

## Upload Status

### iOS Upload Status
- Archive build: DONE for build `10006`
- App Store Connect IPA export/upload: DONE
- Processing status: uploaded package is processing in App Store Connect
- Notes:
  - Build `10006` uploaded successfully on 2026-04-26 via `xcodebuild -exportArchive` with `destination=upload`.
  - Build `10005` uploaded successfully on 2026-04-26 via `xcodebuild -exportArchive` with `destination=upload`.
  - Xcode reported missing dSYMs for Firebase binary frameworks during symbol upload; app package upload still succeeded.
  - Previous build `10005` was superseded by build `10006` after the AI callable entitlement fix and smoke test.
  - Previous build `10004` was superseded by build `10005` for the public Video Hub player cleanup.
  - Previous build `10003` had resolved the Apple AppIcon alpha rejection (`90717`) by converting active AppIcon PNGs to opaque RGB.

### Google Upload Status
- Release AAB build: DONE for versionCode `10014`
- Play upload automation: CONFIGURED via Fastlane `upload_android_internal`
- CLI upload attempt: BLOCKED
- Blocker details:
  - Fastlane requires `SUPPLY_JSON_KEY` to point at a Google Play service-account JSON key.
  - Current shell has no `SUPPLY_JSON_KEY`, so Play Console upload must be manual or rerun after exporting that environment variable.

## Execution Log (Direct Upload Run)

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
3. Verify subscription product setup status for iOS build `10006` and Android versionCode `10014`.
4. Update production Firestore `appConfig/legalContent` and `appConfig/commerceSettings` if old remote operator/legal values still exist.
5. Firestore/Storage rules were deployed on 2026-04-25; fixed owner Firebase Auth account was verified with `emailVerified=true`.
6. Verify data safety/privacy forms reflect actual SDK usage:
   - Auth/account: email, user ID/login state, account identifiers.
   - App activity/analytics: app open, onboarding, signup, membership views, plan selection, purchase/restore outcomes. These are linked to the signed-in UID in `recordAiMembershipEvent`; Android also logs selected events through Firebase Analytics.
   - Purchases/subscriptions: StoreKit/Google Play product IDs, transaction or purchase references, entitlement/restore status.
   - User content: profile data, uploads, AI prompts/outputs, media/gallery selections and support/workflow requests where a user submits them.
   - Device/app data: app version, platform, security/App Check/abuse signals, Firebase installation or app instance identifiers. Advertising ID and AdServices permissions are intentionally removed from Android release manifests.
   - Not used by current binaries: precise/coarse location, camera capture, microphone, contacts, calendar. Photo/video selection uses system pickers; Android `WRITE_EXTERNAL_STORAGE` is capped to API 28 only for saving generated images.
7. Upload and map final screenshot sets for iPhone and Android phone form factors.
8. Set age rating/content rating questionnaires in both consoles.
9. Build numbers are current for the 2026-04-26 upload set: iOS `10006`, Android `10014`.

## Go/No-Go Checklist

- [ ] iOS upload visible in App Store Connect TestFlight
- [ ] Android release visible in Play Console internal/closed track
- [ ] Privacy, terms, support URLs point to final public domain
- [ ] Legal text approved for store/public use
- [ ] Subscription metadata, pricing, and restore behavior validated
- [ ] Data safety/privacy questionnaire completed and consistent with app behavior
- [ ] Content/Age rating completed
- [ ] Screenshots and listing metadata complete in target languages
- [ ] Internal smoke test passed on real iOS and Android devices from uploaded store artifacts

## Exact Next Clicks: App Store Connect

1. Open [App Store Connect](https://appstoreconnect.apple.com/).
2. Go to **My Apps** -> select/create app for bundle `com.skydown.ios`.
3. Open **TestFlight** tab.
4. Under **Builds**, click **+** (or wait for uploaded build).
5. Wait for uploaded build `10006` to finish processing, then attach it to Internal Testers.
6. When build appears, assign to Internal Testers first.
7. Fill **App Information** and **App Privacy** sections.
8. Fill **Pricing and Availability** (manual business decision required).
9. Fill **App Review Information** with support contact + test account.
10. Save draft; do not submit for external/public review without explicit approval.

## Exact Next Clicks: Google Play Console

1. Open [Google Play Console](https://play.google.com/console/).
2. Select app with package `com.nash.skyos` (or create it if missing).
3. Go to **Testing** -> **Internal testing** (or **Closed testing**).
4. Create/Edit release and upload `androidApp/build/outputs/bundle/release/androidApp-release.aab` (versionCode `10014`).
5. Add release notes and save.
6. Go to **Store presence** and complete store listing fields.
7. Go to **App content** and complete:
   - Privacy policy URL
   - Data safety
   - Ads declaration (if applicable)
   - Content rating questionnaire
8. Go to **Monetize** for in-app products/subscriptions validation.
9. Roll out to internal/closed testers only; do not start production rollout without explicit approval.
