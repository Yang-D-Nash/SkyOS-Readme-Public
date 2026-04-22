# SkyOS Legal Overview

## Zweck

Dieses Dokument beschreibt, welche Rechtstexte in SkyOS vorhanden sind, wo sie eingebunden sind und wie sie vor Release geprueft werden.

## In-App Legal Center

In `Settings` sind folgende Dokumente abrufbar:

- FAQ / Guide
- AGB / Terms and Conditions
- Datenschutz / Privacy Policy
- Nutzungsbedingungen / Terms of Service
- Subscription Terms
- AI Usage Notice
- Impressum / Company Info

## Textquellen

- iOS: `LegalContentSettings` + `LegalTextTemplateFactory`
- Android: `SettingsLegalDocumentType.resolve(...)` + `LegalContentSettings`
- Repo-Referenztexte: `docs/legal/`

## Betriebsprozess

1. Owner pflegt Basisangaben (Betreiber, Kontakt, Last Updated, Impressum-Hinweis).
2. In-App Darstellung auf iOS und Android pruefen.
3. Rechtsabnahme dokumentieren.
4. Release erst nach externer juristischer Freigabe.

## Verbindlicher Hinweis

Die Rechtstexte im Repository sind produktionsnahe Vorlagen und Betriebsfassung, ersetzen aber keine abschliessende Rechtsberatung.
