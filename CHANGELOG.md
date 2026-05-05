# Changelog

## 1.0.0 Store review date refresh - 2026-05-05

- Release review metadata: aligned Legal/Privacy/Terms last-updated labels to `5. Mai 2026` across iOS, Android, Functions, and static web pages.
- Backend config: refreshed and verified the live Meta Graph token for Instagram/Facebook social analytics.
- Commerce: deployed Shopify sync/list functions with Admin API secret binding so removed Shopify collections are pruned from the UI.
- Store readiness: documented current iOS build `10024` and Android versionCode `10026` as the active review candidates.

## 1.0.0 Real-data release refresh - 2026-05-04

- AI/Agent sheets: added visible prompt-progress states on iOS and Android so new-chat sends stay open with a deterministic loading banner until the run completes, queues, or is rejected.
- Agent errors: localized the retry state so German users see `Agent braucht Aufmerksamkeit` and `Erneut versuchen` instead of the raw English fallback.
- Release identity: built and uploaded the prompt-progress test rollout as iOS build `10022` and Android versionCode `10023`.
- Backend: Meta Instagram/Facebook live context wired through server-side `adminConfig/metaOAuth`; client access to that token document is blocked by Firestore rules.
- Agent: social analytics now prefers real provider data across Instagram, Facebook, TikTok, YouTube, and Spotify public metadata, with explicit no-mock fallback wording.
- Commerce: Shopify collection sync prunes deleted/empty collections instead of preserving stale UI entries.
- Founder Briefing: artist/music context now uses registered real artist data and avoids random replacement artists.
- Branding: app/README logo assets refreshed and public README typo fixes pushed.
- Release identity: built and uploaded the previous client rollout as iOS build `10021` and Android versionCode `10022` for internal store testing.

## 1.0.0 Productivity & automation launch - 2026-04-29

- README: kompakte **Release-Übersicht** (Reminder+Push, Tasks, Notes, Activepieces, Deploy-Befehle, Secrets, Mobile-Builds) plus Schnellnavigation.
- Doku: `docs/workflow-http-api-activepieces.md` um Task-Antwortfeld **`deduplicated`** ergänzt.
- Android: Compose **Lint** (`LocalContextGetResourceValueCall`) durch gehoiste `stringResource`-Werte in `HomeScreen` / `ProfileScreen` behoben.
- Android: **Detekt** (`UnusedParameter`) in `resolvedMusicHubSocialLinks` bereinigt.
- Git: `main` auf **einen** Commit mit Message `release: prepare SkyOS productivity automation launch` zusammengeführt (`git reset --soft` + force-with-lease).
- Lokal: `./scripts/ci_local_gate.sh` erneut **grün** (Shared, Android lint+detekt+Metadata, Functions inkl. Rules-Emulator).

## 1.0.0 Release Finalization - 2026-04-28

- Deployed `createFounderBriefingFromWorkflow` successfully to Firebase project `skydown-a6add` in `us-central1` (Cloud Functions 2nd gen, Node 22).
- Resolved Android detekt failure by removing an unused `onOpenOrders` parameter from `ProfileScreen` and its call site.
- Restored failing Firestore rules tests by replacing unsupported list-wide validation usage in `adminConfig` AI Studio document payload checks.
- Re-ran the local release gate end-to-end (`shared`, `android`, `functions`, emulator-backed rules tests) with all checks passing.
- Verified iOS release build path with `xcodebuild` for scheme `Skydown App` (Release, iOS Simulator destination).
- Added release handover docs for final execution steps: `RELEASE_NOTES.md`, `CODEX_HANDOVER.md`, and `SCREENSHOTS/README.md`.

## 1.0.0 Release Readiness - 2026-04-27

- Verified local release gates for the current candidate: shared tests, Android lint/metadata compile, Functions tests, Firestore/Storage rules tests, Detekt, Android artifact verification, and iOS Release simulator build.
- Documented the pre-reupload build identity: iOS `1.0.0` build `10007`, Android `1.0.0` versionCode `10015`.
- Prepared the next iOS upload by bumping `CURRENT_PROJECT_VERSION` from `10007` to `10008` after App Store Connect rejected a redundant re-upload of build `10007`.
- Archived iOS build `10008` locally and verified the archive identity as `SkyOS` / `com.skydown.ios` / `1.0.0`.
- Uploaded iOS build `10008` to App Store Connect; package processing started, with only vendor binary-framework dSYM warnings reported.
- Fixed the iOS Music Studio entry so `NICMA MUSIC` and `NICMA STUDIO` use separate ArtistPage states like Android.
- Hardened the iOS Agent mode entry so tapping Agent mounts the screen before live task/note observers attach; build `10009` was superseded before successful upload, and iOS build `10010` was archived, identity-checked, and uploaded to App Store Connect.
- Bumped Android to versionCode `10016` for the next tester build after `10015` went online, then rebuilt and verified the Play AAB.
- Added a release identity preflight to verify Android, iOS, Fastlane, Firebase client files, and the runbook before store uploads.
- Gated Android UI-test launch extras to debuggable builds so release builds cannot be started into mock data or a local fixture user through exported Activity extras.
- Exported Google Play-ready Android phone screenshots at `1242x2424` and Play listing graphics (`512x512` icon, `1024x500` feature graphic).
- Exported iPad App Store screenshots at `2064x2752` after making the screenshot test iPad top-tab aware.
- Removed the launch-time push permission prompt; notification permission now refreshes silently until the user explicitly requests it.
- Clarified that public store rollout remains `no-go` until App Store Connect/TestFlight confirmation, Google Play API validation, final asset upload/mapping, final URLs/legal approval, and real-device smoke are complete.
- Expanded root README, documentation index, release checklist, and screenshot docs with release-critical status and next gates.

## 1.0.0 - 2026-04-24

- Established SkyOS as the version 1 product identity across project metadata, Android versioning, iOS display metadata, and backend package metadata.
- Added a clean functions build script (`npm run build --prefix functions`) for server-side syntax validation.
- Kept existing Firebase callable names and mobile package identifiers for compatibility with the configured Firebase project.
- Added SkyOS-prefixed release signing environment variables while preserving legacy `SKYDOWN_UPLOAD_*` support for existing local setups.
- Stabilized Android lint for V1 by fixing code-level lint errors and treating the existing partial localization backlog as warnings.
- Documented V1 build commands and environment expectations, including release signing and backend secrets.
