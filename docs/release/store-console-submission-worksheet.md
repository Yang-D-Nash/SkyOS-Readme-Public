# Store Console Submission Worksheet

Last local release audit: 2026-05-17 CEST.

Use this worksheet while filling App Store Connect and Google Play Console. It is intentionally
practical: copy the relevant wording, then confirm the account-specific fields in the consoles.

## Quick go-live order

1. `./scripts/release_identity_check.sh` (must pass)
2. Build/upload iOS `22kydwn 1.0.0 (10032)` and Android `22kydwn 1.0.0 (10036)`
3. Paste descriptions from `docs/store-listing.md` (EN + DE)
4. Map screenshots; confirm first frame shows `22kydwn`
5. Set privacy/terms/support URLs (below)
6. Sandbox purchase + restore smoke
7. Submit for review / promote internal draft when QA is green

## Current Build Identity

| Platform | Store identity | Shipped app label | Version |
| --- | --- | --- | --- |
| iOS | `com.skydown.ios` | `22kydwn` | `1.0.0` build `10032` |
| Android | `com.nash.skyos` | `22kydwn` | `1.0.0` versionCode `10036` |

Android note: versionCode `10034` is the completed Play internal hotfix with Samsung Fold smoke
evidence. The previously shipped Android versionCode `10033` is superseded for future widening.

Public product naming decision:

- Store product name: `22kydwn`
- Launcher label: `22kydwn`
- Operator / creative ecosystem: `Skydown`
- Sub-brands in product copy: `Skydown Entertainment` (video), `ZweiZwei` (music), existing merch line marks in shop
- Reviewer note:

```text
22kydwn is the store product and shipped launcher name (umbrella brand). Skydown is the operator. Video is branded Skydown Entertainment; music is branded ZweiZwei; merch uses the existing Skydown / ZweiZwei / combination line artwork. Internal bundle/package names may still contain Skydown/skyos identifiers for release stability.
```

## Public URLs

Use the final approved production URLs. The current hosted Firebase URLs respond publicly:

- Privacy: `https://skydown-a6add.web.app/privacy`
- Terms: `https://skydown-a6add.web.app/terms`
- Support: `https://skydown-a6add.web.app/support`

If a custom domain is intended, do not submit production until the custom-domain versions replace
these URLs in App Store Connect and Play Console.

## App Review Notes

```text
Core review path: launch the app, sign in or create an account, then review AI, Music, Video, Shop, Orders, Profile, and Settings. Legal, support, privacy, account deletion, and restore/subscription surfaces are available in Settings.

The backend for Firebase Auth, Firestore, Storage, Cloud Functions, App Check, AI, commerce, and membership state is live for review. If subscription products are attached, use Apple sandbox test users / Google license testing for purchase and restore verification.

22kydwn is the umbrella store product and launcher name. Skydown is the operator. Video: Skydown Entertainment. Music: ZweiZwei. Merch: existing line identities in Shop.
```

Reviewer access decision still required:

- Preferred: provide a dedicated reviewer account or approved demo mode.
- If relying on Google sign-in/sign-up, verify the reviewer can create a new account without manual approval.

## Google Play Data Safety Draft

Declare only what is true in the submitted console configuration. Based on the current code and docs,
the app should not be declared as "no data collected."

Likely collected data categories:

- Account/contact identifiers: email address, user ID, login state, profile/account identifiers.
- User content: AI prompts, chat/agent inputs, generated outputs, profile/media uploads, notes, tasks, reminders, support/order context where entered by the user.
- App activity: app open, onboarding, signup, membership views, plan selection, purchase/restore outcomes, AI usage/quota events.
- Purchases: product IDs, transaction or purchase references, entitlement and restore status.
- Diagnostics/security: Crashlytics/crash logs, App Check/security signals, device/app integrity signals.
- Files/photos/media selected by user: only when the user chooses uploads or media assets.

Primary purposes:

- App functionality
- Account management
- Purchases/subscription entitlement
- Analytics/product improvement
- Fraud prevention, security, and compliance
- Customer support

Security/privacy answers to verify in console:

- Data is encrypted in transit.
- Account deletion is available in-app via Settings and calls `deleteCurrentUserAccount`.
- Privacy policy URL is provided.
- Reflect Firebase/Google SDK behavior and any third-party SDK collection.

## Google Play Exact Alarm Check

The current Android manifest does not declare `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM`.
Reminder notifications use Android's inexact `setAndAllowWhileIdle(...)` path, so the Play Console
should not require an Exact Alarm permission declaration for this RC.

Before production rollout, verify the generated Play artifact still has no exact-alarm permission
and that reminder screenshots/listing text do not overclaim alarm-clock-level precision.

## Notification Permission Behavior

The current build does not request push notification permission immediately on cold launch. Users are
prompted from notification-related settings/flows instead, and iOS only registers for remote
notifications at launch if authorization was already granted.

## App Store Privacy Draft

Mirror the real behavior in App Store Connect. Likely data types to declare:

- Contact Info: email address, if account email is collected.
- User Content: prompts, generated content, uploaded media, notes/tasks/reminders/support content.
- Identifiers: user ID, purchase/subscription identifiers.
- Purchases: subscription/product/transaction status.
- Usage Data: app interactions, feature usage, membership and restore events.
- Diagnostics: crash data and performance/diagnostic signals.

Tracking: current privacy manifest declares no tracking. Keep App Store Connect aligned with that
unless an SDK/configuration change introduces cross-app tracking.

## Subscription Checklist

Do not submit public production until all are true:

- App Store products exist, are approved/ready for review, and are attached where needed.
- Google Play subscriptions exist and are active for the package/version.
- Pricing, trial, renewal, cancellation, and management text matches the app copy.
- Purchase succeeds in sandbox/license testing.
- Restore succeeds after reinstall/sign-in on iOS and Android.
- Failed purchase and failed restore show clear support path.

## Backend Dependency Audit

The current lockfile includes non-breaking fixes for the advisories that npm could update safely.
Remaining production audit findings are transitive no-fix chains through Genkit / OpenTelemetry /
Google Cloud packages. Treat them as a documented launch-risk acceptance item or track them for a
provider/dependency upgrade before public production rollout.

## Localization Decision

The strict localization guard is green, but the broader audit still reports hardcoded UI literals.

For fastest release:

- Only enable store metadata languages that have been reviewed end-to-end.
- Recommended minimum for this RC: English and German.

For full 10-language launch:

- Complete a full UI localization pass for the remaining hardcoded iOS/Android literals before
  production submission.

## Final Manual Smoke

Run on real devices from uploaded store artifacts:

- Fresh install, launch, sign-in/sign-up, logout/session restore.
- AI text and visual generation success/failure behavior.
- Music, Video, Shop, Cart, Orders, Profile, Settings open without owner/admin leakage.
- Account deletion path is reachable and confirmed.
- Notifications/reminders work or degrade cleanly without permission.
- Subscription purchase/restore paths are verified with sandbox/license testing.
