# SkyOS Privacy

Status: repository operating version for product and implementation alignment. This text should be
reviewed and finalized with qualified counsel before any public launch.

## 1. Controller / Operator

The repository currently identifies the operator contact for SkyOS as:

- `Ngoc Anh Nguyen (Yang D. Nash - Skydown)`
- `Erich-Plate-Weg 44, 22419 Hamburg, Germany`
- `skydownent@gmail.com`

This document describes the current data-handling baseline reflected in the repository and related
compliance notes. Final market-ready privacy disclosures may require additional detail.

## 2. Data We Process

Depending on the active features and the user's actions, SkyOS can process:

- account and authentication data
- profile data
- uploaded media metadata
- AI prompts, outputs, and related usage metadata
- membership and entitlement data
- order, payment, shipping, and support-related data
- legal-consent and support-routing data
- technical logs, runtime status, and abuse-prevention signals

## 3. Why We Process Data

Typical purposes include:

- providing login and account access
- showing and maintaining user profile data
- enabling AI and agent features
- delivering membership and restore behavior
- operating merch orders and payment confirmation
- securing the service and limiting abuse
- responding to support, legal, and account requests
- maintaining product reliability and release quality

Where required by applicable law, additional consent or disclosure may be needed for specific features.

## 4. Services and Processors Used In The Repo

Depending on what is enabled, the repo currently integrates with or prepares for:

- Firebase / Google Cloud for Auth, Firestore, Storage, Cloud Functions, and App Check
- Apple App Store and Google Play for subscription and distribution flows
- Shopify for catalog and commerce operations
- Stripe and Klarna for hosted payment flows where active
- Genkit / Gemini-backed AI execution
- optional external systems such as Activepieces, `n8n`, or Manus BYOS

Some of these providers may act as processors, and some may have their own independent platform role.
See the compliance register in `docs/compliance/` for the current internal view.

## 5. AI and Automation Requests

If AI, automation, or BYOS features are used:

- relevant request content may be sent to the configured runtime or external provider
- usage may be logged for quotas, supportability, and abuse prevention
- prompts should not contain unnecessary secrets or high-sensitivity data
- account-scoped external automation settings should remain isolated per user

## 6. Legal Bases and Regional Notes

Where GDPR or similar frameworks apply, processing can rely on one or more of:

- contract performance
- consent where separately requested
- legitimate interests such as platform security, supportability, and abuse prevention
- legal obligations related to accounting, tax, or regulatory duties

Final release disclosures should map legal bases precisely for the active markets and features.

## 7. Retention

Retention in SkyOS is purpose-based and feature-dependent. Examples visible in the repo include:

- account-related data retained while the account is active, plus any required legal or technical period
- AI history retention controlled by plan-aware settings where implemented
- order and payment-related data retained as required for fulfillment and legal obligations
- technical logs retained only as long as reasonably needed for security and operations

## 8. Sharing and Transfers

Data is shared only to the extent required to provide the enabled service, process payments,
fulfill orders, secure the platform, or satisfy legal obligations. International transfers may
occur depending on the providers in use. Final release disclosures should document the active
transfer basis for each enabled provider.

## 9. Security

The repo includes several concrete safeguards:

- role-aware access control
- Firestore and Storage rules
- upload slot validation
- App Check
- runtime lockdown and write-disable controls
- server-side authority for privileged mutations
- secret management outside the repo for sensitive credentials

No security measure is absolute, but SkyOS is designed to avoid treating UI hiding as real security.

## 10. User Rights

Depending on applicable law, users may have rights such as access, correction, deletion, objection,
restriction, portability, and complaint rights. Operational handling notes for these requests live in
the compliance materials under `docs/compliance/`.

## 11. Contact and Deletion Requests

For the current repository baseline, privacy and deletion requests should be routed to:

- `skydownent@gmail.com`

Before public launch, the final production support and legal routing must be confirmed in the
active operator setup.
