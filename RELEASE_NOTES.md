# Release Notes - 1.0.0

## 2026-05-07 Premium rollout candidate

Current prepared client identity:

- iOS: `1.0.0` build `10026`
- Android: `1.0.0` versionCode `10029`

What changed since the previous store-review candidate:

- Premium brand system added across shared KMP UI, Android surfaces, and iOS settings/admin controls.
- Tokenized color, spacing, typography, radius, elevation, button, card, input, empty, loading, and motion patterns documented for maintainable rollout.
- Public README mirror synchronized with the premium positioning and transparency model.

Verification status:

- Release identity check: pass (`./scripts/release_identity_check.sh`, 2026-05-07)
- iOS archive present locally for build `10026`
- iOS simulator compile check: pass (`xcodebuild ... CODE_SIGNING_ALLOWED=NO build`, 2026-05-07)
- Android release gate: pass (`./scripts/android_release_gate.sh`, versionCode `10029`, AAB/APK rebuilt 2026-05-07 08:56 CEST)
- Android Play AAB SHA-256: `661e20416b626c43481bd6813935e381539e43c0abab44b3df7e8287cb8435b7`
- Android QA APK SHA-256: `3033c5227a4abe9e3a3c4d37ac4ec717a47e062bd7cc88c4b1958a24ad8cca8f`
- Store upload for iOS build `10026` / Android versionCode `10029`: pending manual store-console upload/review step

## 2026-05-05 store review candidate

## What was finalized

- Connected Meta-backed Instagram/Facebook social analytics server-side without exposing tokens to clients.
- Refreshed release-facing Legal/Privacy/Terms dates to `5. Mai 2026` across iOS, Android, Functions, and static site fallbacks.
- Verified the current Meta Graph token against Instagram Business Discovery, the connected Instagram Business account, and the Facebook Page.
- Deployed Shopify sync/list functions with the required Admin API secret binding so deleted Shopify collections prune from the UI.
- Refreshed app/logo assets and release documentation for the next iOS/Android rollout.
- Hardened real-data paths for social analytics, Shopify collections, and Founder Briefing artist context.
- Deployed `createFounderBriefingFromWorkflow` to Firebase project `skydown-a6add` (Cloud Functions 2nd gen, `us-central1`).
- Fixed Android static analysis failure by removing an unused `ProfileScreen` parameter and call-site argument.
- Fixed Firestore rules test breakage caused by unsupported list-wide predicate usage and restored passing rules test coverage.
- Re-ran local quality gates across shared, Android, and Functions modules until all checks passed.
- Verified iOS release build with Xcode on simulator destination (`iPhone 17`).

## Verification summary

- Shared tests: pass (`:shared:allTests`)
- Android checks: pass (`:androidApp:lintDebug`, `detektAll`, `:shared:compileKotlinMetadata`)
- Android release gate: pass (`./scripts/android_release_gate.sh`, versionCode `10026`, AAB/APK rebuilt 2026-05-05 18:30 CEST)
- Google Play internal upload: pass (`fastlane android upload_android_internal`, draft, versionCode `10026`, 2026-05-05 18:33 CEST)
- Functions checks: pass (`npm run build`, `npm test`, Firestore+Storage emulator rules tests)
- iOS build: pass (`xcodebuild ... -configuration Release ... build`)
- iOS App Store Connect upload: pass (`xcodebuild -exportArchive`, build `10024`, package processing started 2026-05-05 18:57 CEST)
- Backend live config: pass (Meta OAuth token live, Shopify sync/list functions deployed with Admin secret, legal content date live at `5. Mai 2026`)

## Release-impacting notes

- Cloud Function URL:
  - `https://us-central1-skydown-a6add.cloudfunctions.net/createFounderBriefingFromWorkflow`
- Firebase secrets required for this deployment were provisioned:
  - `AGENT_RUN_CALLBACK_SECRET`
  - `SKYOS_WORKFLOW_SECRET`

## Known non-critical follow-up

- `functions` local install emits `EBADENGINE` warnings on Node `v24` because `functions/package.json` declares Node `22`; deployment/runtime target remains Node `22`.
