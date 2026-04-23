# SkyOS Developer Guide

This guide explains how to work on SkyOS without breaking release trust. SkyOS is a native iOS and Android app with Firebase backend services and shared Kotlin models.

## Repository Structure

| Path | Purpose |
| --- | --- |
| `Skydown App/` | iOS SwiftUI app, services, models, view models, and views |
| `androidApp/` | Android Compose app, repositories, screens, view models, and platform services |
| `shared/` | Kotlin Multiplatform shared models and domain helpers |
| `functions/` | Firebase Cloud Functions, tests, package metadata, and server logic |
| `firestore.rules` | Firestore access rules |
| `storage.rules` | Storage access rules |
| `docs/` | Product, architecture, legal, localization, and release documentation |
| `scripts/` | Audits, QA helpers, and local utilities |

## Local Requirements

Install or provide:

- Xcode for iOS builds
- Android Studio or command-line Android SDK for Android builds
- JDK compatible with the Gradle setup
- Node.js compatible with `functions/package.json`
- npm for Functions dependencies
- Firebase CLI for emulator/rules tests through local `node_modules`

The current Functions package declares Node 22. Running on another major version can work locally but should not be treated as release parity.

## iOS Setup

1. Open `Skydown App.xcodeproj` in Xcode when visual inspection is needed.
2. Confirm `GoogleService-Info.plist` is present for the intended environment.
3. Build from the repository root:

```bash
xcodebuild -project "Skydown App.xcodeproj" -scheme "Skydown App" -configuration Debug -destination "generic/platform=iOS Simulator" build
```

4. For release, also run a real-device smoke test and archive/signing validation.

## Android Setup

1. Confirm Android SDK and JDK are configured.
2. Confirm `androidApp/google-services.json` matches the intended Firebase project.
3. Build from the repository root:

```bash
./gradlew :androidApp:assembleDebug
```

4. For release, also run a real-device smoke test and signed release build validation.

## Firebase And Functions

Install Functions dependencies:

```bash
npm ci --prefix functions
```

Run Functions and rules tests:

```bash
npm test --prefix functions
```

Current test script runs:

- `npm run test:node`
- `npm run test:rules`

Rules tests use Firebase emulators and create only local emulator data.

## Build Commands

| Check | Command |
| --- | --- |
| Android Debug | `./gradlew :androidApp:assembleDebug` |
| iOS Debug Simulator | `xcodebuild -project "Skydown App.xcodeproj" -scheme "Skydown App" -configuration Debug -destination "generic/platform=iOS Simulator" build` |
| Functions tests | `npm test --prefix functions` |
| Node tests only | `npm run test:node --prefix functions` |
| Rules tests only | `npm run test:rules --prefix functions` |
| Localization audit | `./scripts/localization_audit.sh` |
| Production dependency audit | `npm audit --prefix functions --omit=dev` |

## Deploy Flow

Use a staged deploy process:

1. Review diff and affected data paths.
2. Run Functions and rules tests when backend or rules changed.
3. Run iOS and Android builds when shared models, clients, or navigation changed.
4. Run localization audit for user-facing text changes.
5. Confirm legal/support impact.
6. Deploy to a non-production or controlled environment first when possible.
7. Verify smoke flows.
8. Deploy production with rollback notes.

## Config And Secrets

Never commit:

- service account JSON
- private signing keys
- store API secrets
- Stripe secret keys
- OpenAI or provider API keys
- raw production exports
- `.env` files containing secrets

Firebase client config files identify the Firebase project but do not grant privileged access by themselves. Still, keep environment separation clear and avoid mixing staging and production config by accident.

## Permissions

Security is layered:

- UI hides controls the user should not use.
- View models/repositories avoid unnecessary privileged calls.
- Cloud Functions validate privileged mutations.
- Firestore/Storage rules enforce final client access boundaries.

If a normal user can see an owner/admin action, treat it as a bug even if rules block the write. If rules permit the action, treat it as a release blocker.

## QA Notes

Always include these checks for release-sensitive changes:

- app start and session restore
- login/logout
- Home opens
- AI and Agent open
- Membership opens and restore path is visible
- Profile opens
- Settings opens
- Legal Center opens
- Shop/cart/order path still loads
- owner/admin controls are role-gated
- offline/error states are readable
- no duplicated destructive actions

## Dependency Hygiene

Run:

```bash
npm audit --prefix functions --omit=dev
```

Do not apply forced dependency upgrades without checking breaking changes. If a forced audit fix changes major behavior, classify it as a release risk and test Functions deeply before shipping.

## Live Data Hygiene

Do not delete live Firebase data without:

- audit
- owner approval
- backup or export when useful
- dry run
- targeted deletion
- verification
- written record of what changed

Test data should be clearly prefixed with `qa_`, `test_`, `releasecheck_`, or `deviceqa_`.
