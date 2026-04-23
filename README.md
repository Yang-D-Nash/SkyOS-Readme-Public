# SkyOS

SkyOS is a premium creator operating system for iOS and Android. It brings Home, AI, Music, Video, Shop, Orders, Profile, Membership, Settings, Legal, and Operations into one product surface instead of treating them as isolated features.

The product goal is simple: SkyOS should feel calm, capable, trustworthy, and ready for real users. The codebase is organized around native client quality, Firebase-backed access control, membership-aware AI, creator media workflows, and owner/admin operations.

## Why SkyOS

SkyOS exists for creators and teams who need more than a content app. It combines:

- a daily home surface with signals and utility actions
- AI workflows for ideation, production support, and agent-style execution
- music and video hubs for creator-owned media
- commerce, cart, checkout, and order visibility
- profile and identity management
- membership, entitlement, and usage-aware access
- owner/admin tooling for revenue, support, hygiene, and legal operations

The operating principle is product trust. Users should always understand what is happening, what they can do next, and where to find help.

## Product Areas

| Area | Purpose |
| --- | --- |
| Home | Daily operating surface, status signals, and fast entry points |
| AI | Bot and agent workflows with usage, membership, and safety context |
| Music | Tracks, artist pages, beat hub, and music-related creator surfaces |
| Video | Video hub and supported media presentation |
| Shop | Product discovery, cart, checkout entry, and order submission |
| Orders | User-visible order status and owner-only order management |
| Profile | User identity, dashboard, gallery, and creator presentation |
| Membership | Native purchase/restore flows, plan status, and entitlement refresh |
| Settings | Account, support, legal, AI controls, admin, owner, and system surfaces |
| Owner/Admin | Revenue ops, recommendation lifecycle, user management, and hygiene controls |

## Platforms

- iOS: SwiftUI app in `Skydown App/`
- Android: Jetpack Compose app in `androidApp/`
- Shared model layer: Kotlin Multiplatform in `shared/`
- Backend: Firebase Auth, Firestore, Storage, Cloud Functions, App Check, and security rules
- Documentation: product, role, developer, architecture, localization, legal, and release docs in `docs/`

## Product Philosophy

SkyOS favors:

- clear states over vague loading
- calm upgrade and billing language over pressure
- explicit permissions over hidden assumptions
- single-source operational data over duplicated UI truth
- native platform conventions over ornamental UI
- legal and support visibility as first-class trust paths

SkyOS avoids:

- unlabelled admin power
- destructive actions without confirmation
- silent backend failures
- hardcoded secrets
- unfinished legal paths
- fake release readiness

## Quick Start

From the repository root:

```bash
./gradlew :androidApp:assembleDebug
```

```bash
xcodebuild -project "Skydown App.xcodeproj" -scheme "Skydown App" -configuration Debug -destination "generic/platform=iOS Simulator" build
```

```bash
npm ci --prefix functions
npm test --prefix functions
```

For localization visibility:

```bash
./scripts/localization_audit.sh
```

## Architecture Overview

The clients are native and use Firebase as the operational backbone.

- Auth owns identity and session state.
- Firestore stores users, profiles, content, orders, legal content, membership operations, analytics mirrors, and owner/admin state.
- Cloud Functions own privileged mutations, membership sync, AI guardrails, revenue operations, payment/order actions, and server-side validation.
- Firestore and Storage rules enforce client-side read/write boundaries.
- Shared models keep Android and backend-facing domain contracts aligned where practical.

More detail lives in `docs/ARCHITECTURE.md`.

## Roles

| Role | Scope |
| --- | --- |
| User | Normal app usage, profile, AI, shop, own orders, settings, support, legal |
| Creator/Subadmin | Creator-oriented workflows and limited delegated capabilities where configured |
| Admin | Support, moderation, selected settings, user assistance, controlled operations |
| Owner | Full platform operations, revenue center, recommendations, legal content, runtime controls |

Role behavior is enforced in UI, Functions, and Firestore/Storage rules. UI visibility alone is not treated as security.

## Security And Privacy

- Do not commit private keys, service account files, store credentials, or raw production exports.
- Firebase client config files may contain public Firebase identifiers; treat them as configuration, not secret authority.
- Privileged data changes should happen through callable Functions or owner-only rule paths.
- Legal documents and support paths must remain reachable from Settings.
- Account deletion, restore, membership refresh, and support language should stay calm and explicit.
- Live data cleanup requires audit, backup when appropriate, dry run, targeted deletion, and verification.

## Localization

SkyOS has locale folders for iOS and Android, but full UI localization is not complete while hardcoded UI literals remain in source. The localization guide defines tiers, fallback expectations, terminology, and audit workflow.

Current localization audit:

```bash
./scripts/localization_audit.sh
```

See `docs/LOCALIZATION_GUIDE.md` and `docs/localization-audit.md`.

## Documentation

- `docs/USER_GUIDE.md`
- `docs/CREATOR_GUIDE.md`
- `docs/ADMIN_GUIDE.md`
- `docs/OWNER_GUIDE.md`
- `docs/DEVELOPER_GUIDE.md`
- `docs/ARCHITECTURE.md`
- `docs/LOCALIZATION_GUIDE.md`
- `docs/LEGAL_OVERVIEW.md`
- `docs/RELEASE_CHECKLIST.md`
- `docs/legal/`

## Current Status

Status as of 2026-04-22:

- Android Debug build: verified.
- iOS Simulator Debug build: verified.
- Functions Node tests and Firestore/Storage rules tests: verified.
- Legal foundation: present in docs and in-app legal center structure, pending external legal review before public launch.
- Live Firebase cleanup: not performed without live credentials and an approved deletion plan.
- Release readiness: suitable as an internal release-candidate baseline, not approved for external store release until live billing, live Firebase, device smoke tests, dependency audit decisions, and legal review are complete.

## Legal Notice

The legal documents in this repository are professional product templates and operating references. They do not replace external legal review. Before a public release, qualified legal counsel must approve the final Terms, Privacy Policy, Subscription Terms, AI Usage Notice, Impressum/Company Info, and support/company details for the target markets.
