# VVT - Verzeichnis von Verarbeitungstaetigkeiten (SkyOS / Skydown)

Rechtsgrundlage:
- Art. 30 DSGVO

Hinweis:
- Das ist eine operative Vorlage.
- Pro Aktivitaet Verantwortlichen, Zweck und Fristen konkretisieren.

---

## 1) Stammdaten

- Verantwortlicher: `Nguyen Phuong Ngoc Anh (Yang D. Nash - Skydown)`
- Anschrift: `Erich-Plate-Weg 44, 22419 Hamburg, Deutschland`
- Produkt: `SkyOS / Skydown`
- Kontakt Datenschutzanfragen: `skydownent@gmail.com`
- Version: `v1`
- Stand: `2026-04-29`

---

## 2) Verarbeitungstaetigkeiten

| Taetigkeit | Zweck | Kategorien betroffener Personen | Datenkategorien | Rechtsgrundlage | Empfaenger/Verarbeiter | Speicherfrist | Sicherheitsmassnahmen |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Registrierung und Login | Kontozugang | App-User | Email, Username, Auth UID, Login-Metadaten | Art. 6 Abs. 1 lit. b DSGVO | Firebase Auth | bis Konto-Loeschung + technische Frist | Auth + Zugriffskontrollen |
| Profilverwaltung | Nutzerprofil in App | App-User | Username, WhatsApp (optional), Bio, Social Handle, Profilbild-Metadaten | Art. 6 Abs. 1 lit. b | Firestore/Storage | bis Konto-Loeschung | Rollen, Rules, Upload-Slots |
| Consent Management | Nachweis Einwilligung | App-User | Terms/Privacy Acceptance Timestamp, Version, Source, AI Consent | Art. 6 Abs. 1 lit. c/f | Firestore | fuer Nachweisdauer definieren | serverseitige Regeln, Auditfelder |
| KI Bot/Agent Nutzung | Funktionsbereitstellung | App-User | Prompts, Outputs, technische Limits, ggf. Verlauf | Art. 6 Abs. 1 lit. b / lit. a (optional) | Firebase AI / Manus / n8n (konfigurationsabhaengig) | planabhaengig (Retention) | Rate limits, opt-in, access controls |
| Workflow Trigger (n8n) | User-Automation | App-User | Trigger Payload, technische Header | Art. 6 Abs. 1 lit. b | n8n (je User Setup) | nach Workflow/Logs | auth + per-user config separation |
| Commerce/Bestellung (falls aktiv) | Verkauf/Abwicklung | Kunden | Bestell- und Zahlungsbezogene Daten | Art. 6 Abs. 1 lit. b/c | Shopify/Stripe | handels-/steuerrechtlich | provider security + minimal data |
| Betrieb/Sicherheit/Monitoring | Stabilitaet und Missbrauchsschutz | App-User/Tester | Error Events, Security Logs, Runtime Flags | Art. 6 Abs. 1 lit. f | Firebase/Plattformdienste | kurz und zweckgebunden | least privilege, lock-down modes |

---

## 3) Loeschkonzept (Kurz)

- Account-Loeschung:
  - Auth-Account entfernen
  - zugeordnete Profildaten entfernen
  - Galerie/Uploads nach definiertem Pfad entfernen

- KI-Verlaeufe:
  - Retention nach Plan (`1/3/7/30 Tage`)
  - anschliessend loeschen oder anonymisieren

- Logs:
  - nur so lange wie fuer Security/Betrieb erforderlich

---

## 4) Offene Punkte vor finaler Rechtsfreigabe

- [ ] Datenschutzkontakt final benennen
- [ ] Speicherfristen je Kategorie finalisieren
- [ ] Drittlandtransfers je Dienst final dokumentieren
- [ ] Rechtsgrundlagen je optionales Feature final validieren
