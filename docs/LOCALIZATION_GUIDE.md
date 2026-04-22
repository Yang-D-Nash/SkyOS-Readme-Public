# SkyOS Localization Guide

## Ziel

Localization ist Produktqualitaet. Jede Sprache muss vertrauenswuerdig, klar und layout-sicher wirken.

## Sprachsystem

- iOS: `Localizable.strings` pro Locale
- Android: `strings.xml` in `values-*`
- Einheitliche Begriffe fuer Membership, Plan, Agent, Bot, Workflow

## Key-Regeln

- Neue sichtbare Texte nicht hardcoden.
- Aussagekraeftige, stabile Key-Namen verwenden.
- Fallbacks ruhig und professionell formulieren.

## Tier-Strategie

- Tier-1: EN, DE, ES, FR, PT (Premium-Qualitaet)
- Tier-2: IT, NL, TR, PL, JA (saubere Fallback-Stabilitaet, dann Ausbau)

## Glossar

- Siehe `docs/localization-terminology-glossary.md`
- Keine zufaelligen Synonyme fuer Kernbegriffe verwenden.
