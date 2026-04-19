# Anwalt-Freigabe Checkliste (22xSky)

Stand: `2026-04-19`  
Zweck: 1-Seiten-Pruefblatt fuer die juristische Endfreigabe vor dem oeffentlichen Release.

## Produkt

- App: `22xSky`
- Plattformen: `iOS` und `Android`
- Aktueller iOS QA-Stand: `Build 35 erfolgreich in TestFlight`
- Aktueller Android QA-Stand: `Release-Bundle + Lint lokal erfolgreich`
- Oeffentlicher Release: `noch nicht live`

## Gewuenschte Entscheidung der Kanzlei

Bitte eine klare Einstufung:

- [ ] `Freigabe`
- [ ] `Freigabe mit Auflagen`
- [ ] `Keine Freigabe`

Bitte zusaetzlich alle Pflichtaenderungen nach Prioritaet markieren:

- [ ] `Blocker vor Release`
- [ ] `Vor Release dringend`
- [ ] `Kann direkt nach Release nachgezogen werden`

## Was rechtlich geprueft werden soll

- [ ] Datenschutzerklaerung fuer den aktuellen App- und Backend-Betrieb tragfaehig
- [ ] AGB / Impressum / Verbraucherinformationen ausreichend
- [ ] Consent-Logik rechtlich ausreichend:
  Pflichtzustimmung zu AGB + Datenschutz bei Registrierung,
  separater KI-Opt-in,
  Consent-Nachweis im Backend
- [ ] Rollen der eingesetzten Dienste korrekt eingeordnet
- [ ] AVV/DPA/SCC fuer aktive Dienste ausreichend oder es bestehen konkrete Nachweisluecken
- [ ] Speicherfristen, Loeschkonzept und Betroffenenrechte ausreichend beschrieben
- [ ] KI-/Profiling-/Marketing-Risiken benoetigen keine zusaetzlichen Pflichttexte oder Einwilligungen
- [ ] Keine No-Go-Risiken fuer oeffentlichen App-Release

## Dienste, die besonders relevant sind

- [ ] Firebase: Auth, Firestore, Storage, Functions, App Check
- [ ] Google Sign-In
- [ ] Stripe
- [ ] Shopify
- [ ] Apple App Store Connect / TestFlight
- [ ] Google Play
- [ ] Manus / Agent-Flow nur falls produktiv aktiviert

## Bereits bekannter offener Status

- [ ] AVV/DPA/SCC fuer aktive Dienste final auf `OK`
- [ ] DSAR-Probelauf fuer Auskunft und Loeschung dokumentiert
- [ ] Monitoring / Incident / Recovery-Nachweise dokumentiert
- [ ] Android Release-Signing separat noch technisch offen

## Unterlagen fuer die Pruefung

- `docs/compliance/RELEASE_READINESS_2026-04-19.md`
- `docs/compliance/DSGVO_RELEASE_CHECKLIST.md`
- `docs/compliance/COMPLIANCE_REVIEW_2026-04-15.md`
- `docs/compliance/AVV_VERARBEITER_REGISTER.md`
- `docs/compliance/VVT_VERARBEITUNGSTAETIGKEITEN.md`
- `docs/compliance/BETROFFENENRECHTE_SOP.md`
- `docs/compliance/DATENPANNEN_SOP.md`
- `docs/compliance/TOMS_CHECKLIST.md`

## Gewuenschter Output

- [ ] Klare Release-Empfehlung: `Go` / `Go mit Auflagen` / `No-Go`
- [ ] Liste aller Pflichtanpassungen vor Public Release
- [ ] Konkrete Textaenderungen fuer Datenschutz / AGB / Impressum falls noetig
- [ ] Liste fehlender Vertrags- oder Nachweisdokumente

## Interne Regel

Public Release nur, wenn die Kanzlei keine juristischen Blocker mehr sieht.
