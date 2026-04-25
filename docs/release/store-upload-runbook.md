# SkyOS Store Upload Runbook

Last updated: 2026-04-25 (post-upload hardening)
Owner: Release Engineering

## Build Identity

### iOS
- Bundle ID: `com.skydown.ios`
- Display Name: `SkyOS` (from `SkydownApp-Info.plist`)
- Version: `1.0.0`
- Build: `10002`
- Team ID: `F3BNLG6L7P`

### Android
- Application ID: `com.nash.skyos`
- App Label: `SkyOS`
- versionName: `1.0.0`
- versionCode: `10007`
- Play Billing Library: `8.3.0`

## Build Artifacts

- iOS archive: `build/ios/SkyOS-10002.xcarchive` (`109M`, rebuilt 2026-04-25 13:15 Europe/Berlin)
- iOS export options used: `build/ios/ExportOptions-AppStore.plist`
- iOS IPA: `build/ios/export-10002/SkyOS.ipa` (`26040256` bytes, exported 2026-04-25 13:15:50 Europe/Berlin)
- Android AAB: `androidApp/build/outputs/bundle/release/androidApp-release.aab` (`26110935` bytes, rebuilt 2026-04-25 13:12:16 Europe/Berlin)
- Android APK: `androidApp/build/outputs/apk/release/androidApp-release.apk` (`18997697` bytes, rebuilt 2026-04-25 13:12:14 Europe/Berlin)

## Upload Status

### iOS Upload Status
- Archive build: DONE
- IPA export: DONE
- Apple developer/App Store Connect release: USER-REPORTED DONE
- CLI validation/upload attempt: BLOCKED
- Blocker details:
  - `xcrun altool --list-providers` requires explicit auth params (`--api-key/--api-issuer` or `--username/--app-password/--provider-public-id`)
  - `xcrun iTMSTransporter` reports local Transporter app dependency and cannot proceed in current CLI environment
- CLI upload remains unavailable in this local shell without App Store Connect API credentials or an authenticated Transporter app session

### Google Upload Status
- Release AAB build: DONE
- Play Console release: USER-REPORTED DONE
- Play upload automation: NOT CONFIGURED IN REPO
- Reason: no Google Play Publisher/Fastlane service-account upload pipeline is present
- Future uploads still require manual Play Console upload or a new service-account based automation path

## Execution Log (Direct Upload Run)

1. Re-validated release artifacts exist (`SkyOS.xcarchive`, `SkyOS.ipa`, `androidApp-release.aab`).
2. Rebuilt Android release bundle (`./gradlew :androidApp:bundleRelease`) -> `BUILD SUCCESSFUL`.
3. Attempted Apple CLI validation/upload path:
   - `xcrun altool --validate-app ...` (hang/no usable auth context in this shell session)
   - `xcrun altool --list-providers` -> explicit auth required
   - `xcrun iTMSTransporter -m verify ...` -> local Transporter app requirement message
4. Stopped before any public release submission.

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
- General reliability and navigation polish.

### Google Play release notes
- Unified first-session growth tracking events across startup, onboarding, signup, and first value.
- Stability and trust cleanup across launch and public-facing support/legal surfaces.
- General reliability and navigation polish.

## Manual Remaining Tasks

1. Confirm final legal approval for public privacy/terms wording.
2. Confirm final production domain and replace URL placeholders in App Store Connect and Play Console.
3. Verify subscription product setup status for iOS build `10002` and Android versionCode `10007`.
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
9. Build numbers are bumped for the next upload after post-upload hardening changes.

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
5. Upload `build/ios/export-10002/SkyOS.ipa` using Xcode Organizer or Transporter app after login/2FA.
6. When build appears, assign to Internal Testers first.
7. Fill **App Information** and **App Privacy** sections.
8. Fill **Pricing and Availability** (manual business decision required).
9. Fill **App Review Information** with support contact + test account.
10. Save draft; do not submit for external/public review without explicit approval.

## Exact Next Clicks: Google Play Console

1. Open [Google Play Console](https://play.google.com/console/).
2. Select app with package `com.nash.skyos` (or create it if missing).
3. Go to **Testing** -> **Internal testing** (or **Closed testing**).
4. Create/Edit release and upload `androidApp/build/outputs/bundle/release/androidApp-release.aab`.
5. Add release notes and save.
6. Go to **Store presence** and complete store listing fields.
7. Go to **App content** and complete:
   - Privacy policy URL
   - Data safety
   - Ads declaration (if applicable)
   - Content rating questionnaire
8. Go to **Monetize** for in-app products/subscriptions validation.
9. Roll out to internal/closed testers only; do not start production rollout without explicit approval.
