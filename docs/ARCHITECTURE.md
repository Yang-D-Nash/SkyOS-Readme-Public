# SkyOS Architecture

SkyOS is a native mobile product backed by Firebase. The system is built around clear client state, server-side authority for privileged work, and explicit access boundaries.

## App Overview

SkyOS contains:

- iOS SwiftUI client
- Android Jetpack Compose client
- Kotlin Multiplatform shared model layer
- Firebase Auth for identity
- Firestore for app, user, content, legal, order, analytics, membership, and operations data
- Firebase Storage for media
- Cloud Functions for privileged mutations and server-side workflows
- Firestore and Storage rules for client access enforcement

## Client Layers

The clients generally follow this shape:

- View: native UI surface and user interaction
- ViewModel/Store: state, loading, retry, and command orchestration
- Repository/Service: Firebase, Functions, media, billing, and platform APIs
- Model: domain structures used by UI and transport

Release principle: views may hide actions, but repositories, Functions, and rules must still enforce permission boundaries.

## Core Data Flows

### Auth And Session

1. User signs in through the supported auth path.
2. Auth state restores on app start.
3. User document and role/plan context load.
4. UI gates role-specific surfaces.
5. Functions and rules remain the final authority for privileged behavior.

### Profile

1. User opens Profile.
2. Client reads own user/profile data.
3. User edits allowed fields.
4. Firestore rules validate owner identity and payload shape.
5. UI refreshes local state after write success.

### Shop And Orders

1. User adds items to cart.
2. Checkout/order submission sends validated payload to Functions.
3. Function creates server-owned order data with `orderOwnerUid`.
4. User reads only own orders.
5. Owner reads and manages global orders.
6. Rules prevent normal users from updating or deleting orders.

### Legal

1. Settings exposes Legal Center.
2. Client resolves legal document type.
3. Static/default legal content or configured legal content is shown.
4. Owner can update approved legal content through controlled paths.
5. External legal review remains required before launch.

## Entitlements

Entitlements decide what the user can access.

Important concepts:

- canonical entitlement state should be preferred over stale UI memory
- store purchase/restore paths must refresh membership state
- usage limits should produce calm, readable deny/degrade metadata
- owner/admin roles must not accidentally grant paid user capabilities unless intended

Entitlement bugs can be release-blocking when they grant or deny paid access incorrectly.

## Membership System

Membership combines:

- native purchase/restore UI
- plan display
- usage and capability limits
- entitlement refresh
- analytics and revenue operations
- owner command center recommendations

The app should never imply that a plan changed before the entitlement state is confirmed.

## AI Pipeline

AI flow:

1. User sends prompt.
2. Client prepares request and current membership context.
3. Function/server logic validates access, quota, and guardrails.
4. Provider call runs when allowed.
5. Usage and analytics state update.
6. UI shows result, progress, retry, warning, or membership hint.

AI denial should be useful:

- explain the reason
- suggest the next step
- avoid raw provider errors
- avoid stale warnings after refresh

## Analytics

Analytics supports product operations and revenue decisions.

Tracked areas can include:

- membership opens
- purchase starts/completions
- upgrade hints after denied usage
- restore attempts
- recommendations
- experiment lifecycle
- command center alerts

Analytics should not contain raw secrets or unnecessary sensitive user content.

## Revenue Ops

Revenue Ops is owner-facing and should be auditable.

Core lifecycle:

1. Signal appears.
2. Recommendation is generated or surfaced.
3. Owner starts, rejects, or defers.
4. Experiment or action records timeline events.
5. Learnings are saved.
6. Stale state is completed or cleaned.

The system should prefer fewer useful recommendations over many noisy ones.

## Permissions

Permission layers:

- role model in user/session data
- UI gating
- repository/service scope
- callable Function auth checks
- Firestore rules
- Storage rules

High-risk surfaces:

- orders
- membership and billing
- role/user management
- legal content
- AI usage and quota
- adminConfig
- recommendations and experiments
- media/content editing

## Failure And Offline Behavior

SkyOS should handle:

- empty state
- loading state
- permission denied
- offline/read failure
- retryable server error
- stale session
- store restore delay

Silent failure is not acceptable for release-critical flows.

## Release Architecture Standard

A release candidate should prove:

- clients compile
- Functions and rules tests pass
- role-gated paths are enforced by rules
- key flows have stable loading/error/empty states
- legal and support paths are reachable
- docs describe the real system
- live data cleanup is deliberate, not blind
