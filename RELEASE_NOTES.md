# Release Notes - 1.0.0 (2026-05-04 rollout candidate)

Date: 2026-05-04

## What was finalized

- Connected Meta-backed Instagram/Facebook social analytics server-side without exposing tokens to clients.
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
- Android release gate: pass (`./scripts/android_release_gate.sh`, versionCode `10022`, AAB/APK rebuilt 2026-05-04 15:38 CEST)
- Google Play internal upload: pass (`fastlane android upload_android_internal`, draft, versionCode `10022`, 2026-05-04 15:52 CEST)
- Functions checks: pass (`npm run build`, `npm test`, Firestore+Storage emulator rules tests)
- iOS build: pass (`xcodebuild ... -configuration Release ... build`)
- iOS App Store Connect upload: pass (`xcodebuild -exportArchive`, build `10021`, package processing started 2026-05-04 15:47 CEST)

## Release-impacting notes

- Cloud Function URL:
  - `https://us-central1-skydown-a6add.cloudfunctions.net/createFounderBriefingFromWorkflow`
- Firebase secrets required for this deployment were provisioned:
  - `AGENT_RUN_CALLBACK_SECRET`
  - `SKYOS_WORKFLOW_SECRET`

## Known non-critical follow-up

- `functions` local install emits `EBADENGINE` warnings on Node `v24` because `functions/package.json` declares Node `22`; deployment/runtime target remains Node `22`.
