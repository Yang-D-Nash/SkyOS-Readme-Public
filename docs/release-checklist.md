# SkyOS Release Checklist

Use this checklist for every release candidate. SkyOS is only release-ready when product behavior,
trust paths, backend authority, and support readiness all line up at the same time.

## 1. Source and Hygiene

- [ ] `git status` is clean or intentionally scoped
- [ ] no generated dumps, screenshots, logs, or temp exports are staged
- [ ] no secrets, tokens, service accounts, or private keys are staged
- [ ] version, build number, and release notes are aligned

## 2. Build and Test

- [ ] iOS debug build passes
- [ ] Android debug build passes
- [ ] Android public artifacts were produced with `./scripts/android_release_clean_build.sh`
- [ ] release build validation completed for the platform(s) being shipped
- [ ] distributed Android APK/AAB `versionCode` and `versionName` match `androidApp/build.gradle.kts`
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
