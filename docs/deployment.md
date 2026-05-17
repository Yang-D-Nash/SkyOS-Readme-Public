# SkyOS Deployment

SkyOS deployment is more than `firebase deploy`. A production-worthy release combines build
validation, security rules, live config discipline, payment readiness, and rollback planning.

## 1. Pre-Deploy Requirements

Before any deploy:

- review the diff and affected product surfaces
- confirm no secrets or exports are staged
- run the relevant platform builds
- run backend and rules tests when backend scope changed
- confirm whether legal or support text changed

## 2. Local Validation Commands

```bash
npm ci --prefix functions
npm test --prefix functions
./gradlew :androidApp:assembleDebug
xcodebuild -project "Skydown App.xcodeproj" -scheme "Skydown App" -configuration Debug -destination "generic/platform=iOS Simulator" build
./scripts/localization_audit.sh
```

## 3. Firebase Deploys

Functions:

```bash
firebase deploy --only functions
```

Rules:

```bash
firebase deploy --only firestore:rules,storage
```

Selective function deploy:

```bash
firebase deploy --only functions:syncShopifyMerch,functions:startAiSubscriptionCheckout
```

Use selective deploys when the scope is intentionally narrow and validated.

## 4. Client Release Builds

### iOS

- validate signing in Xcode
- run device smoke on the intended release candidate
- archive and export with the correct team/profile setup

### Android

- provide `keystore.properties` or `SKYOS_UPLOAD_*` env vars
- build `assembleRelease` or `bundleRelease`
- validate on at least one real Android device, plus foldables if they are in target scope

## 5. Production Config Discipline

Do not treat live config casually. Confirm:

- Shopify config is aligned with actual store and collection intent
- Stripe and webhook secrets are present only when payment rails are meant to be live
- runtime config is not accidentally left in lock or debug-oriented state
- legal content settings match the current operator and support truth

## 6. Rollback Basics

Operator quick reference: [incident-runbook.md](incident-runbook.md) (scenarios, roles, severity, comms).

SkyOS rollback is not only about git. In an incident:

1. use runtime lockdown or targeted write locks if user protection is urgent
2. disable payment or automation routes if the issue is provider-specific
3. redeploy the last known good backend commit if needed
4. rebuild and redistribute client hotfixes only when runtime controls cannot contain the issue
5. document what changed and what remains degraded

## 7. Post-Deploy Checks

After deploy, verify:

- auth and session restore
- AI callable health
- legal content reads
- role-sensitive owner/admin surfaces
- checkout preparation and webhook health if live
- upload slot flow if media changes shipped

## 8. What Not To Deploy Blindly

Do not blindly deploy:

- billing changes without live store validation
- rule changes without emulator tests
- legal text changes without owner review
- commerce config changes without support awareness
- provider changes without rollback notes

Deployment should make SkyOS more trustworthy, not merely more current.
