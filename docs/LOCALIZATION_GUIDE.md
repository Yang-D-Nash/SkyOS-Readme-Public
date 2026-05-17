# SkyOS Localization Guide

Localization is product trust. Users should not feel that SkyOS is half-translated, randomly translated, or layout-unsafe in critical paths.

## Language System

iOS:

- localized strings live in `*.lproj/Localizable.strings`
- SwiftUI source still contains hardcoded literals in several areas

Android:

- localized strings live in `res/values*/strings.xml`
- Compose source still contains hardcoded literals in several areas

Audit:

```bash
./scripts/localization_audit.sh
```

## Current Tiers

| Tier | Locales | Standard |
| --- | --- | --- |
| Tier 1 | EN, DE, ES, FR, PT | Premium quality for release-critical UI |
| Tier 2 | IT, NL, TR, PL, JA | Stable fallback, then staged expansion |

Tier 1 should cover the visible core journeys before public launch:

- login/session
- Home
- AI and Agent
- Membership
- Shop/cart/orders
- Profile
- Settings
- Legal and support
- delete/logout/billing restore

## Key Rules

Use keys that are:

- stable
- descriptive
- grouped by feature
- reusable where the exact phrase is truly shared
- not coupled to temporary layout

Avoid:

- raw technical strings in UI
- duplicated near-identical keys
- hardcoded billing warnings
- placeholder text in production screens
- machine-looking fallback strings

## Terminology

Use the glossary in `docs/localization-terminology-glossary.md`.

Core terms should remain consistent:

- SkyOS
- Home
- AI
- Agent
- Bot
- Membership
- Plan
- Restore
- Orders
- Support
- Legal Center

Do not switch between terms such as membership/subscription/plan unless the context requires that distinction.

## Fallback Rules

Fallback copy must:

- be understandable
- avoid blame
- tell the user what to do next
- avoid exposing internal error details
- preserve trust in billing, AI, account, and legal flows

Examples of acceptable tone:

- "Bitte melde dich an, um Bestellungen zu laden."
- "Support findest du in den Einstellungen."
- "Der Status konnte gerade nicht aktualisiert werden. Bitte versuche es erneut."

Examples to avoid:

- raw exception names
- provider stack traces
- "failed"
- "invalid payload"
- unexplained "permission denied"

## Layout Safety

Before release, check:

- German labels that run longer than English
- button text in billing and order actions
- legal document titles
- Settings rows
- AI limit cards
- membership plan names
- small devices
- dynamic type/accessibility text size where supported

If a string does not fit, adjust layout or copy intentionally. Do not shrink text globally.

## Audit Interpretation

The audit counts hardcoded literals and locale key coverage. It does not prove translation quality.

Use audit results to:

- find top files by hardcoded UI text
- prioritize Settings, AI, Cart, Orders, Home, Profile, and Legal
- detect missing locale expansion
- keep release notes honest

As of the 2026-04-22 audit, full UI localization is not complete. This is a release risk for a broad multilingual launch and a P2 quality gap for a limited internal release.

## Release Localization Checklist

- Tier 1 critical flows translated and reviewed.
- Legal titles and support routes translated.
- Billing and membership language reviewed by product/legal.
- AI limit and retry messages reviewed.
- No placeholder copy remains in visible release paths.
- Audit output attached to release notes.
