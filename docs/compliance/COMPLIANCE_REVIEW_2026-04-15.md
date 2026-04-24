# Compliance Review (22xSky) - 2026-04-15

Ziel:
- Nachweisbarer DSGVO-/Compliance-Review fuer den aktuellen Projektstand.
- Ergebnisformat ist fuer Figma-Boards vorbereitet.

Scope:
- Code, Firestore/Storage-Sicherheitsregeln, Functions, vorhandene Compliance-Doku.
- Stand: `2026-04-15`.

## Gesamtfazit

- Technischer Sicherheitsstand: `gut`.
- Dokumentationsstand: `gut, aber mit juristischen Restblockern`.
- Release-Readiness aus Compliance-Sicht: `gelb`.
- Formale Blocker bis "final release-ready": AVV/DPA/SCC Abschluss + interne Betriebsnachweise.

## Findings (nach Prioritaet)

### [P1] AVV/DPA/SCC sind noch nicht final abgeschlossen

- Evidenz:
  - `AVV/DPA Status` ist fuer zentrale Verarbeiter nicht auf `OK`, sondern `IN PROGRESS`/`OPEN`.
  - Betriebsregel fordert bei `!= OK` eine Risikomarkierung.
- Referenzen:
  - `docs/compliance/AVV_VERARBEITER_REGISTER.md:25`
  - `docs/compliance/AVV_VERARBEITER_REGISTER.md:26`
  - `docs/compliance/AVV_VERARBEITER_REGISTER.md:54`
- Risiko:
  - Juristischer Release-Blocker trotz stabilem Technikstand.
- Empfehlung:
  - AVV/DPA pro aktivem Dienst final als `OK` dokumentieren (inkl. Vertragspartei, Datum, Ablagepfad, SCC-Mechanismus).

### [P2] Operative TOMs/Nachweise fuer den Live-Betrieb sind noch unvollstaendig

- Evidenz:
  - Offene Punkte bei Monitoring/Alerting, Recovery-Strategie, Incident-Rollen und Team-Drill.
- Referenzen:
  - `docs/compliance/TOMS_CHECKLIST.md:48`
  - `docs/compliance/TOMS_CHECKLIST.md:49`
  - `docs/compliance/TOMS_CHECKLIST.md:68`
  - `docs/compliance/TOMS_CHECKLIST.md:69`
- Risiko:
  - Bei Vorfall fehlen belastbare Betriebsnachweise fuer Art. 32/33 DSGVO.
- Empfehlung:
  - 1x Incident-Drill dokumentieren, Monitoring-Alerts nachweisbar ausloesen, Recovery-Runbook finalisieren.

### [P2] End-to-End Nachweise fuer Betroffenenrechte sind noch nicht vollstaendig abgehakt

- Evidenz:
  - Auskunfts- und Loeschprozess sind dokumentiert, aber als Probelauf noch offen.
- Referenzen:
  - `docs/compliance/DSGVO_RELEASE_CHECKLIST.md:50`
  - `docs/compliance/DSGVO_RELEASE_CHECKLIST.md:51`
- Risiko:
  - Prozess ist konzeptionell vorhanden, aber ohne geuebten Nachweis schwach auditierbar.
- Empfehlung:
  - 2 Testfaelle (Auskunft + Loeschung) mit Ticket-ID, Zeitstempeln und Ergebnis dokumentieren.

### [P2] Account-Loeschung entfernte persoenliches Agent-Profil bisher nicht vollstaendig (jetzt behoben)

- Evidenz:
  - Cleanup wurde um `adminConfig/agentProfile_<uid>` ergaenzt.
- Referenzen:
  - `functions/index.js:1153`
  - `functions/index.js:1154`
- Risiko:
  - Vor Fix blieb ein personenbezogenes Konfigurationsdokument zurueck.
- Status:
  - `BEHOBEN` im aktuellen Stand.

## Positiv nachgewiesen

- Consent-Gate technisch umgesetzt (AGB/Datenschutz Pflicht + KI-Consent + Consent-Metadaten).
- Firestore/Storage Rules getestet und produktiv deployed.
- Runtime App Check fuer Functions steht auf `enforce`.
- iOS Release nutzt in Production keinen Debug-App-Check-Provider.

Nachweisdateien:
- `docs/compliance/DSGVO_RELEASE_CHECKLIST.md`
- `docs/compliance/RELEASE_READINESS_2026-04-15.md`

## Figma Board Pack (Copy/Paste)

### Frame 1 - Executive Summary

- Titel: `22xSky Compliance Review - 2026-04-15`
- Status: `Gelb`
- Kernbotschaft:
  - Technik stabil und weitgehend abgesichert.
  - Juristische und operative Nachweise (AVV/DPA/SCC + TOMs Drill) sind die letzten Pflichtpunkte.

### Frame 2 - Blocker (P1)

- Card 1:
  - Title: `AVV/DPA/SCC nicht final`
  - Severity: `P1`
  - Owner: `Ngoc Anh Nguyen`
  - Due: `vor finalem Store-Release`
  - Evidence:
    - `AVV_VERARBEITER_REGISTER.md:25-30`
  - Done-Definition:
    - Alle aktiven Dienste in Register auf `OK` mit belegter Ablage.

### Frame 3 - High Priority (P2)

- Card 2:
  - Title: `TOMs Betriebsnachweise offen`
  - Severity: `P2`
  - Owner: `Ngoc Anh Nguyen`
  - Evidence:
    - `TOMS_CHECKLIST.md:48-49, 68-69`
  - Done-Definition:
    - Monitoring/Alerting Nachweis + Recovery-Runbook + Incident-Drill dokumentiert.

- Card 3:
  - Title: `DSAR Probelauf offen`
  - Severity: `P2`
  - Owner: `Ngoc Anh Nguyen`
  - Evidence:
    - `DSGVO_RELEASE_CHECKLIST.md:50-51`
  - Done-Definition:
    - Mind. 1 Auskunfts- und 1 Loeschticket dokumentiert.

- Card 4:
  - Title: `Account-Deletion Cleanup vervollstaendigt`
  - Severity: `P2`
  - Owner: `Engineering`
  - Status: `Done`
  - Evidence:
    - `functions/index.js:1154`

### Frame 4 - Decision Gate

- `GO`, wenn:
  - AVV/DPA/SCC fuer aktive Dienste auf `OK` stehen,
  - TOMs-Nachweise dokumentiert sind,
  - DSAR-Probelauf dokumentiert ist.
- `NO-GO`, wenn einer dieser Punkte offen bleibt.

## Empfohlene naechste Schritte (48h)

1. AVV-Register auf `OK` bringen (inkl. Ablage-Nachweise).
2. Incident-Drill + Monitoring-Test dokumentieren.
3. DSAR-Testfaelle (Auskunft/Loeschung) durchlaufen und dokumentieren.
4. Anschliessend juristische Endfreigabe (Kanzlei) einholen.
