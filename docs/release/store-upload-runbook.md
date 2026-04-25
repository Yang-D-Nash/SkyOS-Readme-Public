# SkyOS Store Upload Runbook

Last updated: 2026-04-24 (SKYOS_DIRECT_UPLOAD_19)
Owner: Release Engineering

## Build Identity

### iOS
- Bundle ID: `com.skydown.ios`
- Display Name: `SkyOS` (from `SkydownApp-Info.plist`)
- Version: `1`
- Build: `35`
- Team ID: `F3BNLG6L7P`

### Android
- Application ID: `com.nash.skyos`
- App Label: `SkyOS`
- versionName: `1`
- versionCode: `35`

## Build Artifacts

- iOS archive: `build/ios/SkyOS.xcarchive`
- iOS export options used: `build/ios/ExportOptions-AppStore.plist`
- iOS IPA: `build/ios/export/SkyOS.ipa`
- Android AAB: `androidApp/build/outputs/bundle/release/androidApp-release.aab`

## Upload Status

### iOS Upload Status
- Archive build: DONE
- IPA export: DONE
- CLI validation/upload attempt: BLOCKED
- Blocker details:
  - `xcrun altool --list-providers` requires explicit auth params (`--api-key/--api-issuer` or `--username/--app-password/--provider-public-id`)
  - `xcrun iTMSTransporter` reports local Transporter app dependency and cannot proceed in current CLI environment
- App Store Connect/TestFlight upload: PENDING MANUAL XCODE ORGANIZER OR TRANSPORTER APP SESSION

### Google Upload Status
- Release AAB build: DONE
- Play upload automation: NOT CONFIGURED IN REPO
- Reason: no Google Play Publisher/Fastlane service-account upload pipeline is present
- Play Console internal/closed upload: PENDING MANUAL PLAY CONSOLE SESSION

## Execution Log (Direct Upload Run)

1. Re-validated release artifacts exist (`SkyOS.xcarchive`, `SkyOS.ipa`, `androidApp-release.aab`).
2. Rebuilt Android release bundle (`./gradlew :androidApp:bundleRelease`) -> `BUILD SUCCESSFUL`.
3. Attempted Apple CLI validation/upload path:
   - `xcrun altool --validate-app ...` (hang/no usable auth context in this shell session)
   - `xcrun altool --list-providers` -> explicit auth required
   - `xcrun iTMSTransporter -m verify ...` -> local Transporter app requirement message
4. Stopped before any public release submission.

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
3. Verify subscription product setup status in both stores for build `35`.
4. Verify data safety/privacy forms reflect actual SDK usage (Firebase/Auth/Analytics/Commerce/AI usage).
5. Upload and map final screenshot sets for iPhone and Android phone form factors.
6. Set age rating/content rating questionnaires in both consoles.
7. Decide whether version/build must be bumped if store rejects duplicate build numbers.

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
5. Upload `build/ios/export/SkyOS.ipa` using Xcode Organizer or Transporter app after login/2FA.
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
