# Release Notes - 1.0.0 (handover candidate)

Date: 2026-04-28

## What was finalized

- Deployed `createFounderBriefingFromWorkflow` to Firebase project `skydown-a6add` (Cloud Functions 2nd gen, `us-central1`).
- Fixed Android static analysis failure by removing an unused `ProfileScreen` parameter and call-site argument.
- Fixed Firestore rules test breakage caused by unsupported list-wide predicate usage and restored passing rules test coverage.
- Re-ran local quality gates across shared, Android, and Functions modules until all checks passed.
- Verified iOS release build with Xcode on simulator destination (`iPhone 17`).

## Verification summary

- Shared tests: pass (`:shared:allTests`)
- Android checks: pass (`:androidApp:lintDebug`, `detektAll`, `:shared:compileKotlinMetadata`)
- Functions checks: pass (`npm run build`, `npm test`, Firestore+Storage emulator rules tests)
- iOS build: pass (`xcodebuild ... -configuration Release ... build`)

## Release-impacting notes

- Cloud Function URL:
  - `https://us-central1-skydown-a6add.cloudfunctions.net/createFounderBriefingFromWorkflow`
- Firebase secrets required for this deployment were provisioned:
  - `AGENT_RUN_CALLBACK_SECRET`
  - `SKYOS_WORKFLOW_SECRET`

## Known non-critical follow-up

- `functions` local install emits `EBADENGINE` warnings on Node `v24` because `functions/package.json` declares Node `22`; deployment/runtime target remains Node `22`.
