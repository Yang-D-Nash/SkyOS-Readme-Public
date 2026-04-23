# SkyOS Admin Guide

Admins help keep the product reliable day to day. Admin access is operational, not experimental. Admins should support users, moderate visible areas, handle approved settings work, and escalate risk early.

## Admin Principles

- Act only within assigned permissions.
- Do not use admin access to bypass normal product flows.
- Document meaningful changes.
- Escalate billing, legal, data-loss, security, and owner-level issues.
- Prefer clear user communication over internal jargon.

## Support

Support work starts with context.

Check:

- user email or UID
- platform
- affected area
- role and plan
- last visible error
- recent membership or store action
- whether the issue reproduces after refresh or re-login

Common support areas:

- login and session restore
- membership status mismatch
- restore not reflected
- AI limit or retry state
- order not visible
- profile save failure
- media player issue
- legal or data request

Never request passwords, full card numbers, private keys, or unnecessary personal data.

## Moderation

Moderation should be calm and evidence-based.

Review:

- obvious abuse
- unsafe content
- impersonation risk
- copyright or brand complaints
- harassment or harmful behavior
- profile/media misuse

Escalate to Owner when legal, payment, safety, or account removal consequences are possible.

## Settings Operations

Admins may see operational settings depending on role and permissions.

Before changing a setting:

- confirm the setting is within admin scope
- understand user impact
- check whether a release or owner approval is required
- document what changed and why
- verify the affected screen after saving

Do not make live runtime changes to solve cosmetic complaints unless the change is already approved.

## Legal Updates

Admins may assist with legal publishing but should not invent legal text.

Process:

1. Receive approved source text.
2. Confirm document type and language.
3. Update through the approved owner/admin path.
4. Verify iOS and Android display.
5. Record date, source, and reviewer.

If text is unclear, expired, or incomplete, escalate instead of improvising.

## User Management

If user management is available:

- verify identity before changing access
- do not assign owner role casually
- apply least privilege
- record the reason for role or limit changes
- confirm that the user document and custom claims are synchronized where applicable

Role changes should be tested by reloading the session or refreshing claims.

## Billing And Membership Support

Billing support must respect platform ownership.

Check:

- platform store account
- visible plan in SkyOS
- native subscription state where available
- restore path
- entitlement refresh
- recent purchase timestamps

If money moved but SkyOS does not reflect the plan, escalate as P1 until resolved. If permissions or paid access are incorrectly granted or denied at scale, escalate as P0.

## Admin Release Duties

During release QA, admins can help verify:

- Settings opens
- Support route is visible
- legal documents load
- account actions are clear
- role-gated areas are hidden from normal users
- admin-only controls do not leak
- user-facing error messages are understandable
