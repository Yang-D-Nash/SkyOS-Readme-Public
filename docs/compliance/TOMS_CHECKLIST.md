# TOMs Checklist (SkyOS / Skydown)

TOMs = Technische und organisatorische Massnahmen nach Art. 32 DSGVO.

Status:
- [x] initial
- [x] aktualisiert am: `2026-04-29`
- [x] freigegeben durch: `Nguyen Phuong Ngoc Anh (Yang D. Nash - Skydown)` (technischer Stand)

---

## 1) Zugriffskontrolle

- [x] Rollenmodell aktiv (`owner/admin/subadmin/user`)
- [x] Least-Privilege umgesetzt
- [x] Serverseitige Claims-Sync validiert
- [ ] Regelmaessige Rechtepruefung als wiederkehrender Betriebsprozess geplant

## 2) Authentifizierung

- [x] Sichere Login-Verfahren aktiv
- [x] Session/Token-Handling geprueft
- [ ] Account-Loeschung technisch verfuegbar und auf beiden Plattformen end-to-end getestet

## 3) Transport- und Speichersicherheit

- [x] TLS/HTTPS fuer alle externen Calls
- [x] Secrets nicht im Client hardcoded
- [x] Mobile Keys lokal sicher gespeichert (Keychain/Keystore)
- [x] Backup-Schutz Android aktiv (`allowBackup=false`)

## 4) Datenminimierung

- [x] nur erforderliche Pflichtdaten fuer Kernprozesse
- [x] optionale Felder klar markiert
- [x] unnoetige Logs/Build-Artefakte im Repo reduziert

## 5) Trennung und Mandantenfaehigkeit

- [x] User-Daten strikt pro UID getrennt
- [x] n8n-/Agent-Konfiguration pro User getrennt
- [x] keine Cross-User-Schreibpfade ohne Berechtigung

## 6) Integritaet und Verfuegbarkeit

- [x] Firestore/Storage Rules mit deny-by-default
- [x] Incident-Lockdown vorhanden
- [ ] Monitoring/Alerting fuer kritische Fehler final produktiv verdrahtet
- [ ] Restore-/Recovery-Strategie final dokumentiert

## 7) Einwilligung und Nachweis

- [x] AGB + Datenschutz Pflicht bei Registrierung
- [x] KI-Consent separat steuerbar
- [x] Consent-Metadaten versioniert gespeichert
- [x] Widerruf in Settings moeglich

## 8) Auftragsverarbeitung

- [x] AVV-Register gepflegt
- [ ] Vertraege und SCC final dokumentiert
- [ ] Unterauftragsverarbeiter final dokumentiert

## 9) Prozesse und Schulung

- [x] Betroffenenrechte-SOP vorhanden
- [x] Datenpannen-SOP vorhanden
- [ ] Incident-Rollen intern final geklaert
- [ ] Team weiss, wie Lockdown/Fallback genutzt wird (kurzer Drill)

---

## Rest-Risiken / TODO

- Risiko:
  - Massnahme:
  - Owner:
  - Zieltermin:
