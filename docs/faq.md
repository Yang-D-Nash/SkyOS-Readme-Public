# SkyOS FAQ

This FAQ is written for real users, partners, and support-facing teammates. It answers the questions
that matter most for trust, billing, AI, orders, and support.

## What is SkyOS?

SkyOS is a native mobile platform that combines AI, creator media, merch commerce, membership,
and owner-operated product controls into one system for the Skydown ecosystem.

## Do I need a membership to use SkyOS?

Not always. Some parts of SkyOS can remain open on a free tier or limited account, while deeper AI
or workflow capability can depend on the active plan and current runtime settings.

## What does membership unlock?

Membership is designed around capability, not token packs. Depending on the active plan, it can
increase AI reach, workflow depth, usage limits, and access to premium outputs or support paths.

## How do I restore purchases?

Restore should be available from the relevant membership area in Settings or AI-related upgrade
flows. If restore does not resolve the expected plan, contact support with the purchase context and
platform used.

## How does merch checkout work?

SkyOS can prepare merch orders in-app and hand off payment through configured provider flows. The
exact live method depends on what the owner team has enabled for the current release.

## Can I track my order in the app?

SkyOS is designed to surface order visibility in-app for the signed-in customer. Owner/admin roles
can see broader order operations, but normal users should only see their own order data.

## Does SkyOS use external AI or automation providers?

Yes, depending on the active feature path. SkyOS can use Firebase-backed AI execution and may also
route optional work through configured external systems such as Activepieces, `n8n`, or Manus BYOS.
Those integrations should be transparent and governed by account scope and runtime rules.

## Which workflow use cases are live today?

Reminder, Tasks, and Notes are live in SkyOS.

If a user says, "Remind me tomorrow at 9 about the dentist", the flow can run through
Activepieces, create the reminder in Firebase, and trigger scheduled due-reminder processing
that delivers push notifications on iPhone and Android once the signed-in app has synced its
Firebase Messaging push token.

Current status:

- Live: Reminder plus automatic push delivery
- Live: Tasks for app capture, storage, management, and Activepieces creation
- Live: Notes for app capture, storage, management, and Activepieces creation
- Coming next: longer-lived profile memory and deeper follow-up automations

## Should I paste sensitive information into prompts?

No. Only share what is needed for the task. Do not paste private credentials, payment secrets, or
other high-sensitivity material into prompts or automation fields.

## Who can access owner or admin controls?

Owner and admin controls are restricted by role and enforced beyond the UI. If a normal account can
see or trigger privileged controls, that should be treated as a product bug.

## What happens if AI or billing is temporarily unavailable?

SkyOS should explain blocked or degraded states clearly rather than failing silently. If a feature
is unavailable because of limits, billing state, or runtime restrictions, the product should point
to the next useful step.

## How do I contact support?

Current repository support contact: `skydownent@gmail.com`

Before public launch, support ownership, response expectations, and legal contact routing should be
confirmed in the final operator setup.

## How do I request account deletion or privacy help?

Use the in-app support or legal path when available, or contact the operator via the listed support
email. The repo also contains compliance process documents for deletion and rights handling.
