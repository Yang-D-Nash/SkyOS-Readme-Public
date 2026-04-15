# AVV Verarbeiter-Register (22xSky)

Zweck:
- Uebersicht aller externen Dienste, die personenbezogene Daten verarbeiten.
- Nachweis, ob AVV/DPA abgeschlossen ist und wo er abgelegt ist.

Hinweis:
- Felder als laufendes Register pflegen.
- Ohne geklaerten AVV-Status keinen produktiven Einsatz.

---

## 1) Register-Tabelle

| Dienst | Rolle | Datenkategorien | Zweck | Region/Drittland | AVV/DPA Status | SCC/Transfer | TOMs geprueft | Vertrag Link/Ablage | Owner intern |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Google Firebase (Auth/Firestore/Storage/Functions/Remote Config/App Check) | Auftragsverarbeiter | Accountdaten, Metadaten, Upload-Metadaten, Nutzungsdaten | Core App Betrieb | EU/US (anbieterabhaengig) | TODO | TODO | TODO | TODO | TODO |
| Google Sign-In | Auftragsverarbeiter / eigener Verantwortlicher Anteil pruefen | OAuth Profildaten (minimal) | Login | EU/US | TODO | TODO | TODO | TODO | TODO |
| n8n Cloud (falls genutzt) | Auftragsverarbeiter (je Setup) | Workflow-Payloads | Automation | Anbieterabhaengig | TODO | TODO | TODO | TODO | TODO |
| Manus API (falls genutzt) | Auftragsverarbeiter (je Setup) | Prompt- und Ergebnisdaten | Agent Verarbeitung | Anbieterabhaengig | TODO | TODO | TODO | TODO | TODO |
| Stripe (falls aktiv) | Eigenstaendiger Verantwortlicher / Auftragsverarbeiter je Use-Case pruefen | Zahlungsdaten, Bestelldaten | Payment | EU/US | TODO | TODO | TODO | TODO | TODO |
| Shopify (falls aktiv) | Eigenstaendiger Verantwortlicher / Auftragsverarbeiter je Use-Case pruefen | Shop-/Bestelldaten | Commerce | Anbieterabhaengig | TODO | TODO | TODO | TODO | TODO |
| Apple App Store Connect / TestFlight | Eigener Verantwortlicher Anteil (Plattform) | Crash-/Nutzungsdaten | Distribution/QA | EU/US | n/a | n/a | TODO | TODO | TODO |
| Google Play Console / Firebase App Distribution | Eigener Verantwortlicher Anteil (Plattform) | Crash-/Nutzungsdaten | Distribution/QA | EU/US | n/a | n/a | TODO | TODO | TODO |

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

