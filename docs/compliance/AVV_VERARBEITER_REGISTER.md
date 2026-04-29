# AVV Verarbeiter-Register (SkyOS / Skydown)

Zweck:
- Uebersicht aller externen Dienste, die personenbezogene Daten verarbeiten.
- Nachweis, ob AVV/DPA abgeschlossen ist und wo er abgelegt ist.

Hinweis:
- Felder als laufendes Register pflegen.
- Ohne geklaerten AVV-Status keinen produktiven Einsatz.

---

## 1) Register-Tabelle

Status-Definition:
- `OK`: rechtlich einsatzbereit und intern belegt.
- `IN PROGRESS`: Dokument/Terms vorhanden, aber interne Nachweise oder Entity-Mapping offen.
- `OPEN`: noch nicht ausreichend belegt.
- `n/a`: kein klassischer AVV-Pfad (Plattform-Eigenrolle), nur Transparenzpflichten.

Stand der Tabelle: `2026-04-29`

| Dienst | Rolle | Datenkategorien | Zweck | Region/Drittland | AVV/DPA Status | SCC/Transfer | TOMs geprueft | Vertrag Link/Ablage | Owner intern | Naechster Schritt |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Google Firebase (Auth/Firestore/Storage/Functions/Remote Config/App Check) | Auftragsverarbeiter | Accountdaten, Metadaten, Upload-Metadaten, Nutzungsdaten | Core App Betrieb | EU/US (anbieterabhaengig) | IN PROGRESS | SCC-Katalog vorhanden | IN PROGRESS | DPST: https://firebase.google.com/terms/data-processing-terms ; SCC: https://firebase.google.com/terms/firebase-sccs | Nguyen Phuong Ngoc Anh | Vertragspartei + internen Nachweis (PDF/Screenshot, Datum) in Legal-Ordner ablegen |
| Google Sign-In | Rollenmix pruefen (OAuth-Dienst) | OAuth-Profildaten (minimal) | Login | EU/US | OPEN | Ueber Google/Firebase Vertragswerk pruefen | OPEN | https://firebase.google.com/terms | Nguyen Phuong Ngoc Anh | Mit Kanzlei final klaeren, ob eigener AVV-Pfad noetig oder ueber Google-Vertragsrahmen abgedeckt |
| n8n Cloud (falls genutzt) | Auftragsverarbeiter | Workflow-Payloads | Automation | Anbieterabhaengig | IN PROGRESS | Anbieterabhaengig (DPA + Subprozessoren) | IN PROGRESS | DPA: https://n8n.io/legal/data-processing-agreement/ ; Subprozessoren: https://n8n.io/legal/sub-processors/ ; Security: https://n8n.io/legal/security/ | Nguyen Phuong Ngoc Anh | DPA-PDF unterschreiben und an legal@n8n.io senden; gegengezeichnete Fassung ablegen |
| Manus API (falls genutzt) | Auftragsverarbeiter (zu bestaetigen) | Prompt-/Antwortdaten | Agent Verarbeitung | Anbieterabhaengig | IN PROGRESS | Transferbasis ueber DPA/Privacy pruefen | OPEN | DPA: https://manus.im/policies/data-processing-addendum ; Terms: https://manus.im/terms ; Privacy: https://manus.im/privacy | Nguyen Phuong Ngoc Anh | Vertraege/Transfermechanismus mit Anbieter dokumentieren und juristisch freigeben lassen |
| Stripe (falls aktiv) | Rollenmix (Processor + teils Controller) | Zahlungsdaten, Bestelldaten | Payment | Global inkl. US | IN PROGRESS | Data Transfers Addendum vorhanden | IN PROGRESS | DPA: https://stripe.com/legal/dpa ; DTA: https://stripe.com/legal/dta | Nguyen Phuong Ngoc Anh | Account-Entity (SPEL/Stripe LLC) im Register fixieren und Ablage der geltenden Fassung dokumentieren |
| Shopify (falls aktiv) | Rollenmix (Processor + teils Controller bei Enhanced Services) | Shop-/Bestelldaten | Commerce | Global (u. a. SG/CA) | IN PROGRESS | GDPR/UK/CH Appendix im DPA | IN PROGRESS | DPA: https://www.shopify.com/legal/dpa | Nguyen Phuong Ngoc Anh | Genutzte Shopify-Services gegen Appendix E pruefen und Rollenhinweise in Datenschutzerklaerung abstimmen |
| Apple App Store Connect / TestFlight | Eigener Verantwortlicher Anteil (Plattform) | Crash-/Nutzungsdaten | Distribution/QA | EU/US | n/a | n/a | IN PROGRESS | Apple Plattformbedingungen/Privacy Dokumentation intern ablegen | Nguyen Phuong Ngoc Anh | Datenschutzhinweis auf Plattformrolle und Datenfluss klar formulieren |
| Google Play Console / Firebase App Distribution | Eigener Verantwortlicher Anteil (Plattform) | Crash-/Nutzungsdaten | Distribution/QA | EU/US | n/a | n/a | IN PROGRESS | Google Plattformbedingungen/Privacy Dokumentation intern ablegen | Nguyen Phuong Ngoc Anh | Datenschutzhinweis auf Plattformrolle und Datenfluss klar formulieren |

---

## 2) Pflichtfelder je Dienst

Vor produktivem Einsatz muessen pro Dienst folgende Punkte vorliegen:

- Vertragspartner (rechtliche Einheit, Adresse)
- Datenkategorien
- Zweck der Verarbeitung
- Speicherort/Region
- Unterauftragsverarbeiter
- AVV/DPA-Dokument inkl. Datum
- SCC/Transfermechanismus (falls Drittlandbezug)
- TOMs/ISO/SOC Nachweise
- Loesch-/Rueckgabeprozess bei Vertragsende

---

## 3) Betriebsregel

Wenn `AVV/DPA Status != OK`, dann:

1. Dienst in Release als Risiko markieren.
2. Nur Testbetrieb ohne produktive personenbezogene Daten.
3. Owner-Freigabe erforderlich.
