# SkyOS Legal Overview

This document maps the legal foundation in SkyOS. It explains what exists, where it is surfaced, and what still requires external legal review.

## Legal Documents

SkyOS maintains reference documents in `docs/legal/`:

- `TERMS_OF_SERVICE.md`
- `PRIVACY_POLICY.md`
- `SUBSCRIPTION_TERMS.md`
- `AI_USAGE_NOTICE.md`
- `IMPRESSUM_COMPANY_INFO.md`

These documents are product-ready foundations, not final legal approval.

## In-App Legal Center

Settings should expose:

- Terms of Service / AGB
- Privacy Policy / Datenschutz
- Subscription Terms
- AI Usage Notice
- Impressum / Company Info
- Support

The legal center is part of user trust. It must remain reachable without hidden owner/admin access.

## Implementation Map

iOS:

- Settings hosts the legal entry points.
- `LegalContentStore` and legal template logic resolve legal text.
- Legal content can be backed by configured content where supported.

Android:

- Settings hosts legal entry points.
- `SettingsLegalDocumentType.resolve(...)` maps legal document types.
- `LegalContentRepository` and legal content models supply document content.

Backend/rules:

- Legal content is publicly readable where intended.
- Owner-only writes are enforced by rules.
- Invalid or oversized legal content is rejected by tests/rules.

## Trust Paths Related To Legal

Legal readiness includes more than legal documents.

Review:

- delete account wording and flow
- support contact
- billing restore
- membership plan labels
- AI usage notice
- privacy/data handling explanations
- account removal and data deletion expectations
- company/contact information
- last-updated dates

## External Review Required

Before public release, qualified legal counsel should review:

- company operator data
- jurisdiction and governing law
- consumer cancellation and withdrawal rights
- subscription renewal and cancellation wording
- store policy alignment for Apple and Google
- AI limitation and acceptable-use language
- privacy disclosures, processors, retention, and user rights
- Impressum requirements for target markets
- support and contact obligations

## Update Process

1. Receive approved legal source text.
2. Confirm document type and version/date.
3. Update repository reference text if needed.
4. Update in-app configured legal content if the app uses remote content.
5. Verify iOS display.
6. Verify Android display.
7. Record reviewer and approval date.
8. Include legal status in release notes.

## Release Classification

Legal foundation is present when:

- all required documents exist
- Settings links exist
- content displays on both platforms
- support is reachable
- owner update path is controlled

Legal release approval is present only when:

- external review is complete
- final operator data is correct
- store metadata matches legal text
- release notes record the approved version

## Disclaimer

The repository contains professional product and operating legal text. It does not provide legal advice and does not replace review by qualified counsel.
