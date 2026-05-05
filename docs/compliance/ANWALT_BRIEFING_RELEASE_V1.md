# Anwalt-Briefing fuer Release V1 (SkyOS / Skydown)

Status: Briefing fuer juristische Begleitpruefung
Datum: `2026-05-05`
Produkt: `SkyOS / Skydown`
Rechteinhaber und Entwickler: `Nguyen Phuong Ngoc Anh (Yang D. Nash - Skydown)`
Anschrift: `Erich-Plate-Weg 44, 22419 Hamburg, Deutschland`

---

## 1) Ziel der Pruefung

Bitte um juristische Endpruefung zur DSGVO-/IT-rechtlichen Release-Freigabe von `SkyOS / Skydown` (iOS + Android) fuer Version `1`.

Wir benoetigen eine klare Einschätzung:

1. Sind Datenschutzhinweise, AGB, Impressum und Einwilligungslogik rechtlich tragfaehig?
2. Sind AVV-/DPA-Anforderungen mit eingesetzten Dienstleistern ausreichend abgedeckt?
3. Gibt es No-Go-Risiken fuer Live-Release?
4. Welche Pflichttexte/Pflichtprozesse muessen vor Release zwingend nachgezogen werden?

---

## 2) Technischer Ist-Stand (kurz)

Folgende Punkte sind technisch bereits umgesetzt:

1. Pflicht-Zustimmung zu AGB + Datenschutz bei Registrierung.
2. Separate KI-Einwilligung (opt-in/opt-out), spaeter in Settings aenderbar.
3. Consent-Nachweisfelder im Backend (Zeitpunkt, Version, Source, KI-Consent-Status).
4. Serverseitige Erzwingung ueber Firestore Rules.
5. Legacy-User-Reparatur fuer fehlende Consent-Felder.
6. Android Backup-Haertung (`allowBackup=false`, Backup-Excludes).

---

## 3) Relevante Unterlagen im Projekt

Bitte diese Dateien als Grundlage verwenden:

1. `docs/compliance/DSGVO_RELEASE_CHECKLIST.md`
2. `docs/compliance/AVV_VERARBEITER_REGISTER.md`
3. `docs/compliance/VVT_VERARBEITUNGSTAETIGKEITEN.md`
4. `docs/compliance/BETROFFENENRECHTE_SOP.md`
5. `docs/compliance/DATENPANNEN_SOP.md`
6. `docs/compliance/TOMS_CHECKLIST.md`

Sowie die in der App verwendeten Rechtstexte (AGB/Datenschutz/Impressum) in der aktuellen Version.

---

## 4) Dienste/Verarbeiter (zu pruefen)

Bitte insbesondere die Rollenabgrenzung und AVV-Pflichten fuer folgende Dienste pruefen:

1. Firebase (Auth, Firestore, Storage, Functions, Remote Config, App Check)
2. Google Sign-In
3. n8n Cloud (falls produktiv aktiv)
4. Manus API (falls produktiv aktiv)
5. Stripe / Shopify (falls produktiv aktiv)
6. Apple/Google Distributionsplattformen (Plattformrolle und Pflichten)

---

## 5) Konkrete Fragen an die Kanzlei

1. Sind die Rechtsgrundlagen je Verarbeitungsvorgang korrekt zugeordnet?
2. Reicht die bestehende Consent-Logik fuer App-Release aus?
3. Gibt es zusaetzliche Einwilligungen/Informationspflichten (z. B. KI, Profiling, Marketing)?
4. Sind die geplanten Speicherfristen und Loeschkonzepte ausreichend?
5. Welche AVV/SCC/Transfer-Dokumente fehlen noch konkret?
6. Gibt es wettbewerbsrechtliche/verbraucherschutzrechtliche Risiken in den App-Texten?
7. Welche Formulierungen muessen vor Live zwingend angepasst werden?

---

## 6) Gewuenschter Output der Kanzlei

Bitte als Ergebnis:

1. `Freigabe` oder `Freigabe mit Auflagen` oder `Keine Freigabe`.
2. Liste aller Pflichtanpassungen (priorisiert: Blocker / Hoch / Mittel).
3. Redline oder konkrete Textvorschlaege fuer AGB, Datenschutz, Impressum.
4. Liste fehlender AVV/DPA-Dokumente inkl. Mindestinhalt.
5. Kurze schriftliche Stellungnahme zur Release-Faehigkeit von Version 1.

---

## 7) Mail-Template an die Kanzlei

Betreff:
`SkyOS / Skydown - Bitte um juristische Endpruefung fuer DSGVO/AVV und Release-Freigabe (Version 1)`

Text:

```text
Hallo [Name/Kanzlei],

wir stehen kurz vor dem Release unserer App SkyOS / Skydown (iOS/Android) und bitten um eine juristische Endpruefung.

Ziel:
- DSGVO-/IT-rechtliche Freigabe fuer Version 1
- Pruefung von AGB, Datenschutzhinweisen, Impressum, Consent-Logik und AVV-Lage

Technischer Stand:
- Pflicht-Zustimmung zu AGB + Datenschutz bei Registrierung
- separate KI-Einwilligung (opt-in/opt-out)
- Consent-Nachweis inkl. Version/Zeiten/Source
- serverseitige Rule-Enforcement und Security-Haertung

Bitte pruefen Sie die beigefuegten Unterlagen und geben Sie uns:
1) Freigabe/Freigabe mit Auflagen/keine Freigabe
2) Pflichtanpassungen priorisiert
3) konkrete Textaenderungen (AGB/Datenschutz/Impressum)
4) fehlende AVV/SCC-Punkte

Unterlagen:
- docs/compliance/DSGVO_RELEASE_CHECKLIST.md
- docs/compliance/AVV_VERARBEITER_REGISTER.md
- docs/compliance/VVT_VERARBEITUNGSTAETIGKEITEN.md
- docs/compliance/BETROFFENENRECHTE_SOP.md
- docs/compliance/DATENPANNEN_SOP.md
- docs/compliance/TOMS_CHECKLIST.md
- aktuelle App-Rechtstexte (AGB/Datenschutz/Impressum)

Vielen Dank und viele Gruesse
Nguyen Phuong Ngoc Anh (Yang D. Nash)
SkyOS / Skydown
```

---

## 8) Interne Entscheidungsregel

Release `nur`, wenn:

1. keine juristischen Blocker offen sind,
2. alle Pflichtanpassungen umgesetzt sind,
3. AVV-/DPA-Luecken geschlossen oder formal belastbar bewertet sind.
