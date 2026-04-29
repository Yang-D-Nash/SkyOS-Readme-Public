# CODEX Handover

## 1) Projektstatus

- Release candidate is in a green local verification state.
- Final iOS UI-test verification passed in a fresh simulator session; iOS build `10018` is uploaded to App Store Connect and processing.
- Critical targeted backend deployment is complete: `createFounderBriefingFromWorkflow` is live on `skydown-a6add`.
- Core local CI gate passes end-to-end.

## 2) Verwendeter Stack

- iOS/iPadOS/Mac Catalyst: SwiftUI (`Skydown App`)
- Android: Kotlin + Jetpack Compose (`androidApp`)
- Shared domain: Kotlin Multiplatform (`shared`)
- Backend: Firebase Auth, Firestore, Storage, Cloud Functions (`functions`)

## 3) Wie starten

- Root checks:
  - `./scripts/ci_local_gate.sh`
- Functions only:
  - `npm ci --prefix functions`
  - `npm run build --prefix functions`
  - `npm test --prefix functions`
- Android:
  - `./gradlew :androidApp:assembleDebug`
- iOS:
  - Open `Skydown App.xcodeproj` and run scheme `Skydown App`

## 4) Wie builden

- Android release artifacts:
  - `./gradlew :androidApp:bundleRelease`
- iOS release build (validated variant):
  - `xcodebuild -project "Skydown App.xcodeproj" -scheme "Skydown App" -configuration Release -sdk iphonesimulator -destination "platform=iOS Simulator,name=iPhone 17" build`
- Functions syntax + tests:
  - `npm run build --prefix functions`
  - `npm test --prefix functions`

## 5) Getestete Flows

- Shared KMP unit/integration path (`:shared:allTests`)
- Android static quality path (`lintDebug`, `detektAll`)
- Functions callable and security behavior (`test:node`)
- Firestore/Storage rules suite under emulators (`test:rules`)
- iOS scheme release build path (`Skydown App`, Release)
- iOS UI-test target (`Skydown AppUITests`) on a fresh iPhone 17 simulator for build `10018`

## 6) Bekannte NICHT kritische Punkte

- Local Node version can be newer than declared Functions engine (`22`) and prints `EBADENGINE` warnings during local `npm ci`; tests/build still pass.
- Firestore emulator logs expected permission-denied traces during negative rules tests (asserted behavior).

## 7) Release command

- Targeted function deploy:
  - `npx firebase-tools deploy --only "functions:createFounderBriefingFromWorkflow" --project skydown-a6add`
- Full functions deploy (if needed):
  - `npx firebase-tools deploy --only functions --project skydown-a6add`

## 8) Upload instructions

- iOS:
  - Follow `docs/release/store-upload-runbook.md` and `docs/release/app-release-workflow.md`.
  - Ensure build/version bump consistency before archive upload.
- Android:
  - Build/sign with configured keystore env values from `.env.example`.
  - Upload AAB through Play Console per runbook/checklist docs.
- Backend:
  - Ensure required secrets exist in `skydown-a6add` before deploy.

## 9) Letzte Änderungen

- Deployed `createFounderBriefingFromWorkflow` to production project.
- Uploaded iOS `1.0.0` / build `10018` to App Store Connect; package processing started.
- Built signed Android `1.0.0` / versionCode `10019` and uploaded it to Google Play internal testing as a draft.
- Fixed Android detekt failure in `ProfileScreen` wiring.
- Fixed Firestore rules expression issue that broke security rules tests.
- Added release-level handover docs (`RELEASE_NOTES.md`, `CODEX_HANDOVER.md`) and screenshot manifest.

## 10) Warum release-ready

- All mandatory local quality gates currently pass.
- iOS release build path is verified.
- Fresh iOS UI-test run passed for build `10018`.
- iOS build `10018` and Android versionCode `10019` are uploaded to their store consoles for internal/TestFlight processing paths.
- Required targeted backend function is deployed and reachable.
- Release/handover documentation is now explicit enough for Codex to execute final store/release actions without additional debugging.
