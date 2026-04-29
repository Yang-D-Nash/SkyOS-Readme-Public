# DSGVO Release Checklist (SkyOS / Skydown)

Status: `Stand 2026-04-29 (Launch Baseline)`

Release:
- App Name: `Skydown`
- System/Produktkern: `SkyOS`
- Version: `1.0.0`
- Datum: `2026-04-29`
- Verantwortlicher/Rechteinhaber: `Nguyen Phuong Ngoc Anh (Yang D. Nash - Skydown), Erich-Plate-Weg 44, 22419 Hamburg`
- Freigegeben von: `Nguyen Phuong Ngoc Anh`

---

## A) Rechts- und Dokumentationsgrundlagen

- [x] Datenschutzerklaerung in App aktuell.
- [x] AGB in App aktuell.
- [x] Impressum/Rechteinhaber aktuell.
- [x] Version/Stand von AGB + Datenschutz in Legal Content gepflegt.
- [x] VVT aktualisiert (`VVT_VERARBEITUNGSTAETIGKEITEN.md`).
- [x] AVV-Register aktualisiert (`AVV_VERARBEITER_REGISTER.md`).

## B) Einwilligung und Transparenz

- [x] Registrierung blockiert ohne Zustimmung zu AGB + Datenschutz.
- [x] KI-Einwilligung separat (opt-in/opt-out) verfuegbar.
- [x] Consent-Metadaten werden gespeichert (Zeitpunkt, Version, Source).
- [x] Consent-Aenderung in Settings fuer User moeglich.
- [ ] Letzter Device-Smoketest auf klares UX-Feedback bei Consent-Fehlern.

## C) Zugriff und Sicherheit

- [x] Firestore Rules getestet (Emulator).
- [x] Storage Rules getestet (Emulator).
- [x] Firestore Rules produktiv deployed und live verifiziert (`firebase deploy --only firestore:rules,storage`, 2026-04-29).
- [x] Storage Rules produktiv deployed und live verifiziert (`firebase deploy --only firestore:rules,storage`, 2026-04-29).
- [x] Runtime-Config live geprueft (`system/runtimeConfig` ist erreichbar; `appCheckMode=enforce`, 2026-04-29).
- [ ] App Check Enforcement im produktiven Modus ohne Debug-Token auf echten Geraeten vollstaendig verifiziert.
- [x] Rollen-/Claim-Sync getestet (owner/admin/subadmin/user).
- [x] Android Backup-Haertung aktiv (`allowBackup=false` + excludes).
- [x] Secrets nicht im Repo (nur Runtime/Secret Manager/Keychain/Keystore).

## D) Betroffenenrechte und Prozesse

- [x] SOP fuer Betroffenenrechte dokumentiert (`BETROFFENENRECHTE_SOP.md`).
- [x] Datenpannen-SOP verfuegbar (`DATENPANNEN_SOP.md`).
- [x] Supportkontakt fuer Datenschutzanfragen veroeffentlicht (`skydownent@gmail.com`).
- [ ] Auskunftsprozess intern als Probelauf einmal komplett durchgespielt.
- [ ] Loeschprozess intern als Probelauf einmal komplett durchgespielt.

## E) Datenminimierung und Speicherdauer

- [x] Nicht benoetigte Artefakte/Logs aus Repo-Basis bereinigt und ignoriert (`.gitignore` gehaertet).
- [x] Upload-Pfade und Metadaten via Rules-Tests geprueft.
- [ ] KI-Historie-Retention pro Plan final business-seitig bestaetigt.
- [ ] Loeschpfade fuer Account + Profil + Medien auf beiden Plattformen manuell smoke-getestet.

## F) Dienstleister / AVV

- [x] AVV-Register gepflegt (`AVV_VERARBEITER_REGISTER.md`).
- [x] n8n/Manus BYOS-Hinweise in Rechtstexten und User-Doku klar.
- [ ] Fuer alle aktiven Verarbeiter AVV/DPA final signiert oder rechtsverbindlich bewertet.
- [ ] Drittlandtransfer je Dienst final geprueft und dokumentiert.
- [ ] Unterauftragsverarbeiter je Dienst final verlinkt/dokumentiert.

## G) Technischer Release-Gate

- [x] Android Build erfolgreich.
- [x] iOS Release Build erfolgreich.
- [x] iOS App Check Release-Haertung aktiv (Release nutzt DeviceCheck statt Debug-Provider).
- [x] Rules Tests erfolgreich (`npm run test:rules`, 42/42 gruen).
- [ ] Vollstaendiger Device-Smoketest: Registrierung, Login, Consent, Rollenwechsel, KI, Upload (iOS + Android).
- [ ] Monitoring/Alerts fuer Prod-Fehler final aktiv und testweise ausgelost.

---

## Freigabeentscheidung

- [ ] `GO`
- [ ] `NO GO`

Kommentar:
