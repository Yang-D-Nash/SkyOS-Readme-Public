# DSGVO Release Checklist (22xSky)

Status: `vor jedem produktiven Release ausfuellen`

Release:
- Version:
- Build iOS:
- Build Android:
- Datum:
- Freigegeben von:

---

## A) Rechts- und Dokumentationsgrundlagen

- [ ] Datenschutzerklaerung in App aktuell.
- [ ] AGB in App aktuell.
- [ ] Impressum/Rechteinhaber aktuell.
- [ ] Version/Stand von AGB + Datenschutz in Legal Content gepflegt.
- [ ] VVT aktualisiert (`VVT_VERARBEITUNGSTAETIGKEITEN.md`).
- [ ] AVV-Register aktualisiert (`AVV_VERARBEITER_REGISTER.md`).

## B) Einwilligung und Transparenz

- [ ] Registrierung blockiert ohne Zustimmung zu AGB + Datenschutz.
- [ ] KI-Einwilligung separat (opt-in/opt-out) verfuegbar.
- [ ] Consent-Metadaten werden gespeichert (Zeitpunkt, Version, Source).
- [ ] Consent-Aenderung in Settings fuer User moeglich.
- [ ] Nutzer bekommen klares Feedback bei Consent-Fehlern.

## C) Zugriff und Sicherheit

- [ ] Firestore Rules deployed und getestet.
- [ ] Storage Rules deployed und getestet.
- [ ] App Check aktiv im produktiven Modus.
- [ ] Rollen-/Claim-Sync getestet (owner/admin/subadmin/user).
- [ ] Android Backup-Haertung aktiv (`allowBackup=false` + excludes).
- [ ] Secrets nicht im Repo (nur Runtime/Secret Manager/Keychain/Keystore).

## D) Betroffenenrechte und Prozesse

- [ ] Prozess fuer Auskunftsanfragen intern getestet.
- [ ] Prozess fuer Loeschanfragen intern getestet.
- [ ] Prozess fuer Berichtigung/Portabilitaet intern getestet.
- [ ] Supportkontakt fuer Datenschutzanfragen veroeffentlicht.
- [ ] Datenpannen-SOP intern verfuegbar (`DATENPANNEN_SOP.md`).

## E) Datenminimierung und Speicherdauer

- [ ] KI-Historie-Retention pro Plan geprueft.
- [ ] Nicht benoetigte Felder/Logs entfernt oder minimiert.
- [ ] Upload-Pfade und Metadaten auf Notwendigkeit geprueft.
- [ ] Loeschpfade fuer Account + Profil + Medien getestet.

## F) Dienstleister / AVV

- [ ] Fuer jeden aktiven Verarbeiter liegt AVV vor oder ist rechtlich geprueft.
- [ ] Drittlandtransfer je Dienst geprueft (falls relevant).
- [ ] Unterauftragsverarbeiter je Dienst dokumentiert.
- [ ] n8n/Manus BYOS-Hinweise in User-Doku klar.

## G) Technischer Release-Gate

- [ ] Android Release Build erfolgreich.
- [ ] iOS Release Build erfolgreich.
- [ ] Rules Tests erfolgreich.
- [ ] Smoke-Tests: Registrierung, Login, Consent, Rollenwechsel, KI, Upload.
- [ ] Monitoring/Alerts aktiv.

---

## Freigabeentscheidung

- [ ] `GO`
- [ ] `NO GO`

Kommentar:

