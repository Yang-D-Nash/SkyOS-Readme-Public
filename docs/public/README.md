# SkyOS

SkyOS is a native creative operating app for music, media, merch, membership, and AI-assisted work.
It brings the artist, community, shop, and creative command layer into one product experience across
Apple and Android platforms.

## Product Quality

SkyOS is being shaped as a premium native product, not a generic mobile shell. The current app uses a
shared brand system across Apple and Android with controlled typography, spacing, color, elevation,
loading states, empty states, icon actions, toggles, segmented controls, and admin surfaces. The goal
is a calm, precise, trustworthy experience for creative work, commerce, membership, and AI workflows.

## What SkyOS Brings Together

| Area | Purpose |
| --- | --- |
| AI Studio | Text, visual, agent, and guided creative workflows |
| Music | Artist profile, tracks, releases, media context, and owner-managed artist page updates |
| Video | Clips, visuals, collaborations, and featured content |
| Merch | Product discovery, checkout entry points, and order support |
| Membership | Plans, entitlements, restore paths, and usage transparency |
| Trust | Privacy, terms, support, and AI disclosure surfaces inside the app |

## Product Principles

- One clear brand system across product, store, and support surfaces.
- Native-first mobile experience with calm navigation, precise hierarchy, and direct actions.
- Premium UI controls instead of generic defaults where product trust and repeat use matter.
- Transparent membership and restore flows before any paid user commitment.
- AI features that keep user intent, limits, and disclosure visible.
- Legal, privacy, and support access that are reachable from the app.
- Technical transparency for builders: the public documentation explains the architecture, rebuild
  path, account requirements, and secret boundaries without publishing private credentials.

## Platform Direction

SkyOS is built for iOS, iPadOS, Android, and Mac Catalyst with shared product logic where it helps
consistency and native UI where it matters for platform quality.

The public product surface is expected to stay visually aligned across platforms while still honoring
native Apple and Android behavior, accessibility, motion preferences, and platform-specific controls.

## Public Status

SkyOS is preparing its first public release. Store availability, supported regions, pricing, and
official policy URLs will be announced through the live store listings and public support channels.

## Current Rollout Update

- Latest rollout candidate keeps app version `1.0.0` and advances build identities to iOS build `10029` and Android versionCode `10032`.
- Music hub artist management was updated to support premium artist-page operations: canonical artist ordering, owner CRUD, and in-app rename flows with clear confirmation feedback.

## Transparency

The technical source of truth for rebuild and review is the repository documentation. It should make
the system understandable without exposing production secrets, private keys, live user data, or store
account access.

For the active release state and exact upload status, use `docs/release/store-upload-runbook.md` as the
canonical source.

## Support

For public support, use the support link provided in the live app listing once SkyOS is available.
