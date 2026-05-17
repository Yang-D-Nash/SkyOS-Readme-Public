# 22kydwn Release Checklist

Use this checklist for every release candidate. 22kydwn is only release-ready when product behavior,
trust paths, backend authority, branding, and support readiness all line up at the same time.
Single source of truth for live release state is `docs/release/store-upload-runbook.md`.

## Current RC Snapshot

Status as of 2026-05-17 CEST:

- Umbrella rebrand to **22kydwn** is in code, legal, store copy, and README.
- Sub-brands: **Skydown Entertainment** (video), **ZweiZwei** (music); merch line artwork unchanged.
- Identity preflight **PASSED**: iOS `22kydwn 1.0.0 (10032)`, Android `22kydwn 1.0.0 (10036)`.
- Store copy: `docs/store-listing.md` · Console paste sheet: `docs/release/store-console-submission-worksheet.md`.

Before upload:

1. `./scripts/release_identity_check.sh`
2. Fresh iOS archive + Android release AAB for the build numbers above
3. Paste store text; map screenshots to `22kydwn`
4. Device smoke: Home, AI, Agent, ZweiZwei, Skydown Entertainment video, Shop, Settings/Legal
5. Trigger or verify public README sync (`Sync public README` workflow on `main`)

## 1. Source and Hygiene

- [ ] `git status` is clean or intentionally scoped
- [ ] no generated dumps, screenshots, logs, or temp exports are staged
- [ ] no secrets, tokens, service accounts, or private keys are staged
- [ ] test-only launch flags, mock repositories, and fixture users are unavailable in release builds
- [ ] version, build number, and release notes are aligned

## 2. Build and Test

- [ ] iOS debug build passes
- [ ] Android debug build passes
- [ ] optional: `./gradlew detektAll` (fails the build on findings; see `config/detekt.yml`)
- [ ] Android public artifacts were produced with `./scripts/android_release_clean_build.sh`
- [ ] immediately before any Play upload, `./scripts/verify_android_release_artifacts.sh` passed (or Fastlane ran it via `validate_android_internal` / `upload_android_internal`)
- [ ] release build validation completed for the platform(s) being shipped
- [ ] distributed Android APK/AAB `versionCode` and `versionName` match `androidApp/build.gradle.kts` (verify script and `aapt2` re-check the APK when `ANDROID_HOME` is set)
- [ ] Functions tests pass
- [ ] Firestore and Storage rules tests pass
- [ ] localization audit has been reviewed for user-facing copy changes

## 3. Core Product Smoke

- [ ] cold launch works
- [ ] session restore works
- [ ] login and logout work
- [ ] Home opens
- [ ] AI, Agent, Music, Video, Shop, Orders, Profile, and Settings open
- [ ] empty, loading, retry, and error states are readable
- [ ] no owner/admin actions leak into normal-user flows

## 4. AI and Agent

- [ ] bot text flow succeeds or fails cleanly
- [ ] visual generation succeeds or fails cleanly
- [ ] agent execution state is understandable
- [ ] quota and membership hints are accurate
- [ ] blocked-state messaging is calm and actionable
- [ ] owner runtime changes have not broken user AI access

## 5. Commerce and Membership

- [ ] merch catalog loads
- [ ] cart updates correctly
- [ ] checkout initiation works
- [ ] order creation and refresh work
- [ ] payment confirmation path is validated if live
- [ ] membership upgrade path is visible
- [ ] restore path is visible and validated
- [ ] support path for billing and orders is visible

## 6. Settings, Legal, and Support

- [ ] legal center opens on both platforms
- [ ] Terms, Privacy, Subscription Terms, AI Usage Notice, and Imprint display correctly
- [ ] support email or support route is visible
- [ ] account deletion path is reviewed
- [ ] no broken localization keys or obvious placeholder copy remain

## 7. Owner and Admin

- [ ] role changes are still properly gated
- [ ] runtime controls load
- [ ] revenue ops and membership dashboard load if included in release scope
- [ ] kill switches and lockdown behavior are understood by the release owner
- [ ] normal users cannot reach privileged Functions or protected documents

## 8. Device Reality

- [ ] at least one real iPhone smoke pass completed for the release candidate
- [ ] at least one real Android phone smoke pass completed for the release candidate
- [ ] foldable smoke completed if foldables are in target scope
- [ ] platform-specific polish issues are accepted or fixed before ship

## 9. Analytics, Crash, and Monitoring

- [ ] analytics events critical to launch are still firing or intentionally unchanged
- [ ] crash monitoring path is active
- [ ] rollback owner knows what to watch after rollout
- [ ] support knows the current release state

## 10. Legal and Compliance

- [ ] operator details are current
- [ ] legal review status is known
- [ ] privacy disclosures match the actually enabled providers
- [ ] store metadata and subscription wording match the in-app product
- [ ] compliance blockers are documented if any remain

## 11. Go / No-Go Rule

Release should proceed only when:

- no P0 issue remains
- P1 issues are fixed or explicitly accepted by owner
- billing and restore behavior are proven on the live-intended path
- legal status is explicit
- the team knows the rollback path

If any of the above is unknown, the correct answer is `no-go`.
