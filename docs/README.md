# SkyOS Documentation

This folder is the working foundation for SkyOS as a product, a codebase, and an operating
system for the team behind it. The goal is simple: a developer, partner, reviewer, investor,
support contact, or founder should be able to understand what SkyOS is, how it is built, and
how it is operated without reverse-engineering the repository.

## Start Here

- [architecture.md](architecture.md) - complete product and system map
- [backend.md](backend.md) - Firebase, Functions, rules, runtime controls, and backend authority
- [ios.md](ios.md) - iOS setup, signing, run, and QA
- [android.md](android.md) - Android setup, Compose build flow, and Fold testing guidance
- [ai-system.md](ai-system.md) - AI assistant, FAQ, visual generation, agent runtime, and safety
- [commerce.md](commerce.md) - merch, orders, checkout, shipping, payment rails, and memberships
- [owner-admin.md](owner-admin.md) - owner workflows, controls, kill switches, and release governance
- [deployment.md](deployment.md) - deploy flow, selective deploys, rollback basics, and post-deploy checks
- [ci.md](ci.md) - CI jobs, path filters, quality gates, and trigger behavior
- [release-checklist.md](release-checklist.md) - release readiness gate across product, legal, analytics, and trust
- [branding.md](branding.md) - product naming, voice, assets, and copy rules
- [workflow-http-api-activepieces.md](workflow-http-api-activepieces.md) - Activepieces HTTP setup for Reminder, Task, and Note creation
- [automation/activepieces-minimal-app-flow.md](automation/activepieces-minimal-app-flow.md) - minimal Activepieces flow for **Agent-Automation** (sync URL, Return Response, app contract)
- [faq.md](faq.md) - user-facing operational answers and support guidance
- [store/README.md](store/README.md) - App Store and Google Play presence, screenshots, and review prep

## Current Release Entry

As of 2026-04-27, SkyOS is documented as a `1.0.0` release candidate for the productivity
automation launch. Reminder + Push, Tasks, Notes, and Activepieces creation endpoints are the live
workflow surface; longer-lived memory and deeper follow-up automations remain coming next. Public
store rollout still depends on store-console, legal, URL, asset upload/mapping, and real-device
smoke items outside the repo.

- [release/app-release-workflow.md](release/app-release-workflow.md) - **step-by-step App Store release** (iOS/Android/backend scope); **maintain this file** when the release process changes
- [release/store-upload-runbook.md](release/store-upload-runbook.md) - current build identity, upload state, blockers, hashes, and next console clicks
- [release-checklist.md](release-checklist.md) - generic go/no-go checklist for every release candidate
- [../manual-test-checklist.md](../manual-test-checklist.md) - role and platform smoke matrix for manual release validation
- [beta-distribution.md](beta-distribution.md) - external tester access path and 5-minute feedback script
- [workflow-http-api-activepieces.md](workflow-http-api-activepieces.md) - server-side HTTP workflow API and secret header contract

## Legal And Trust

- [legal/terms.md](legal/terms.md)
- [legal/privacy.md](legal/privacy.md)
- [legal/imprint.md](legal/imprint.md)
- [legal/SUBSCRIPTION_TERMS.md](legal/SUBSCRIPTION_TERMS.md)
- [legal/AI_USAGE_NOTICE.md](legal/AI_USAGE_NOTICE.md)
- [compliance/README.md](compliance/README.md)

## Supporting References

- [design/SKYOS_EXPERIENCE_PRINCIPLES.md](design/SKYOS_EXPERIENCE_PRINCIPLES.md)
- [localization-terminology-glossary.md](localization-terminology-glossary.md)
- [localization-audit.md](localization-audit.md)
- [localization-roadmap.md](localization-roadmap.md)
- [ai-membership-funnel-metrics.md](ai-membership-funnel-metrics.md)
- [ai-subscriptions-rollout-brief.md](ai-subscriptions-rollout-brief.md)
- [store/app-store.md](store/app-store.md)
- [store/google-play.md](store/google-play.md)
- [store/screenshots.md](store/screenshots.md)
- [store/review-prep.md](store/review-prep.md)

## Compatibility Notes

Older upper-case docs such as `DEVELOPER_GUIDE.md`, `OWNER_GUIDE.md`, `LEGAL_OVERVIEW.md`, and
`RELEASE_CHECKLIST.md` remain in the repository as compatibility entry points. The canonical
versions now live in the lower-case files listed above.

## Operating Standard

If a change affects user trust, product behavior, permissions, billing, legal text, or release
process, the relevant document should be updated in the same pull request or commit. SkyOS should
not rely on tribal knowledge.
