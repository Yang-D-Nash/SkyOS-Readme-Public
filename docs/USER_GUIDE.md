# SkyOS User Guide

SkyOS is designed to feel like a calm operating surface for creator work. Most users should be able to move through Home, AI, Music, Profile, Membership, Settings, Shop, Orders, and Support without needing internal product knowledge.

## First Start

1. Open the app and sign in.
2. Let the session restore before starting a purchase, AI request, or profile change.
3. Use Home as the first orientation point.
4. Open Settings when you need account, membership, legal, or support actions.

If the app shows an offline, loading, or retry state, wait for the action to finish before repeating the same request. SkyOS is built to avoid duplicate work, but repeated taps during a network change can still create confusing expectations.

## Home

Home is the daily entry point.

Use it to:

- read current product signals
- jump into high-priority modules
- continue creator or account work
- find utility actions without digging through settings

Healthy Home behavior:

- cards and signals load without blocking the whole screen
- empty states explain what appears there later
- errors offer a retry or a clear next step
- stale content refreshes after returning to foreground or pulling to refresh where supported

## AI

The AI area provides bot-style support for fast prompts and visual/text assistance. AI is membership-aware, usage-aware, and designed to explain limits calmly.

Use AI for:

- drafts, hooks, ideas, captions, and concept work
- lightweight creative exploration
- visual prompt support where available
- membership-aware suggestions and upgrade context

Good practice:

- do not paste sensitive financial, legal, medical, or private third-party data unless the product explicitly supports that workflow
- review AI output before publishing or sending it
- use retry only after a visible error or timeout
- check Membership or Settings if usage limits appear unexpectedly

## Agent

Agent is for more structured work than the normal AI chat. It is better suited to planning, step-by-step output, workflow thinking, and guided creative production.

Expected states:

- progress states should explain that work is ongoing
- limit states should include a human-readable reason
- retry should be available after recoverable errors
- membership hints should not block normal reading of existing output

## Music

Music contains creator media surfaces such as tracks, artist pages, beat hub, and related content.

Use Music to:

- browse available tracks and media
- open creator pages
- preview or launch supported embedded players
- return to Home or Profile after listening

If a player fails to open, check network state first. Some embedded providers also depend on the external service being available.

## Video

Video surfaces support approved video content and external provider previews where configured.

Expected behavior:

- public videos are readable without owner controls
- owner/admin editing controls should not appear to normal users
- unsupported provider data should fail safely instead of exposing broken players

## Shop And Cart

Shop is used for browsing products and opening product details. Cart stores selected items and starts checkout when checkout is enabled for the current user and runtime state.

Before checkout:

- verify item, size, color, quantity, and price
- review shipping/contact fields
- use the visible checkout or submit action once
- wait for the confirmation or error state before retrying

If checkout is unavailable, SkyOS should explain the state instead of silently failing.

## Orders

Orders show order status for the signed-in user. Normal users can read their own orders. Owner users can manage orders globally.

Users can expect:

- new orders to appear after successful submission and refresh
- payment, shipping, fulfillment, and total details where available
- clear empty states when no orders exist
- support guidance when order data cannot load

Only owner-level users should see actions such as marking payment received, toggling completion, or removing an order.

## Profile

Profile is the user identity and dashboard surface.

Use it to:

- review visible identity information
- manage profile presentation where enabled
- check activity and account context
- access creator-relevant media or quick actions

Profile changes should be saved only after the UI confirms the operation. If a save fails, retry after checking connectivity.

## Membership

Membership controls plan visibility, upgrades, restore, and entitlement refresh. Store billing is handled through native platform paths.

Use Membership to:

- view the current plan
- upgrade where available
- restore purchases after reinstall, device change, or account mismatch
- refresh status after a store action

Important:

- purchase confirmation can take time to sync
- restore depends on the Apple or Google account used for the purchase
- plan labels should match the current entitlement state, not only local UI memory

## Settings

Settings is the trust and control center.

Use it for:

- account and session actions
- membership and billing restore
- AI/account controls
- support
- legal documents
- admin/owner areas when your role allows it

High-trust actions such as logout, account deletion, legal review, support, or billing restore should always be explicit and reversible where the platform allows it.

## Support

Use Support from Settings for account, billing, order, membership, and product questions.

When contacting support, include:

- account email
- platform and app version if visible
- affected area
- time of the issue
- screenshots when safe and useful
- order or purchase reference if relevant

Do not include passwords, private keys, full payment card numbers, or unnecessary third-party personal data.
