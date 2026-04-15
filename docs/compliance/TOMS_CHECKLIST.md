# TOMs Checklist (22xSky)

TOMs = Technische und organisatorische Massnahmen nach Art. 32 DSGVO.

Status:
- [ ] initial
- [ ] aktualisiert am:
- [ ] freigegeben durch:

---

## 1) Zugriffskontrolle

- [ ] Rollenmodell aktiv (`owner/admin/subadmin/user`)
- [ ] Least-Privilege umgesetzt
- [ ] Serverseitige Claims-Sync validiert
- [ ] Regelmaessige Rechtepruefung geplant

## 2) Authentifizierung

- [ ] Sichere Login-Verfahren aktiv
- [ ] Session/Token-Handling geprueft
- [ ] Account-Loeschung technisch verfuegbar

## 3) Transport- und Speichersicherheit

- [ ] TLS/HTTPS fuer alle externen Calls
- [ ] Secrets nicht im Client hardcoded
- [ ] Mobile Keys lokal sicher gespeichert (Keychain/Keystore)
- [ ] Backup-Schutz Android aktiv (`allowBackup=false`)

## 4) Datenminimierung

- [ ] nur erforderliche Pflichtdaten
- [ ] optionale Felder klar markiert
- [ ] unnnoetige Logs reduziert

## 5) Trennung und Mandantenfaehigkeit

- [ ] User-Daten strikt pro UID getrennt
- [ ] n8n-Konfiguration pro User getrennt
- [ ] keine Cross-User-Schreibpfade ohne Berechtigung

## 6) Integritaet und Verfuegbarkeit

- [ ] Firestore/Storage Rules mit deny-by-default
- [ ] Incident-Lockdown vorhanden
- [ ] Monitoring/Alerting fuer kritische Fehler
- [ ] Restore-/Recovery-Strategie dokumentiert

## 7) Einwilligung und Nachweis

- [ ] AGB + Datenschutz Pflicht bei Registrierung
- [ ] KI-Consent separat steuerbar
- [ ] Consent-Metadaten versioniert gespeichert
- [ ] Widerruf in Settings moeglich

## 8) Auftragsverarbeitung

- [ ] AVV-Register gepflegt
- [ ] Vertraege und SCC dokumentiert
- [ ] Unterauftragsverarbeiter dokumentiert

## 9) Prozesse und Schulung

- [ ] Betroffenenrechte-SOP vorhanden
- [ ] Datenpannen-SOP vorhanden
- [ ] Incident-Rollen intern geklaert
- [ ] Team weiss, wie Lockdown/Fallback genutzt wird

---

## Rest-Risiken / TODO

- Risiko:
  - Massnahme:
  - Owner:
  - Zieltermin:

