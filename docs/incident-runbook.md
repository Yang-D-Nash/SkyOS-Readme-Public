# SkyOS / Skydown — Incident Runbook

Kurzes Operativ-Handbuch für produktive Vorfälle. **Grundlage für Rollback und Post-Deploy:** [deployment.md](deployment.md) (Abschnitte **6. Rollback Basics** und **7. Post-Deploy Checks**).

| Begriff | Bedeutung |
| --- | --- |
| **Runtime-Config** | Firestore-Dokument `system/runtimeConfig` — zentrale Notbremse (Lockdown, Writes, Uploads, Registrierungen, App-Check, Budget-Lockdown). Siehe [backend.md](backend.md) Abschnitt 5. |
| **Owner-Callable** | `setRuntimeLockdown` — manuelle Laufzeitkontrolle (nur Owner; siehe [owner-admin.md](owner-admin.md) Abschnitt 6). |
| **Backend-Autorität** | Cloud Functions in `functions/` — u. a. Zahlungs-Webhooks, KI, Commerce. |

---

## Rollen und Verantwortlichkeiten

| Rolle | Aufgabe im Incident |
| --- | --- |
| **Incident Lead (IC)** | Koordination, Entscheidungsfristen, Doku, Abbruchkriterien. |
| **Plattform-Owner** | Freigabe für `setRuntimeLockdown`, schwere Runtime-Config-Änderungen, externe Stakeholder (Revenue/Legal) wenn nötig. |
| **Backend-Ansprech** | Functions-Redeploy, Secrets/Stripe/Shopify-Konsole, Log-Auswertung (Cloud Logging), Webhook-Status. |
| **Client-Release-Owner** | App-Store-Regression, Hotfix-Pfade, ggf. gestufter Rollout. |
| **Support / Comms** | Nutzer-Kommunikation nach vorgegebenem Kanal (kein technisches Raten). |

*Konkrete Namen und Kanäle: internes Betriebshandbuch; nicht in diesem Repo duplizieren.*

---

## Severity (Severity)

| Stufe | Kriterium | Reaktion |
| --- | --- | --- |
| **SEV1** | Zahlungs-/Datenintegrität riskant, weitreichender Ausfall, massiver Abuse oder unkontrollierte Kosten. | Sofort: IC + Owner, Sicherheit first. |
| **SEV2** | Kernfeature gebrochen (z. B. Auth, Checkout, KI-Fluss für viele User), kein sofortiger Datenverlust. | IC, Ziel: Containment innerhalb einer Stunde. |
| **SEV3** | Degradation, begrenzte Nutzergruppe, Workaround existiert. | Geplanter Fix, kein 24/7-Zwang. |
| **SEV4** | Kosmetik / Monitoring-Noise. | Backlog, keine Runbook-Eskalation. |

---

## Kommunikationsweg (immer)

1. **Eintrag** mit Zeitstempel, SEV, Symptom, betroffene Fläche (Zahlung / KI / Auth / Release / Global).
2. **Aktualisierung** alle 30–60 min bei SEV1/2, bis stabil oder eskaliert.
3. **Abschluss** — siehe [Nachbereitung](#nachbereitung-pie).

Externe Nutzer: nur abgestimmte Texte/Statusseite; keine technischen Interna.

---

## Rollback-Entscheidung (Kurzlogik)

1. **Runtime zuerst**, Binary zuletzt: `system/runtimeConfig` und provider-spezifische Abschaltung siehe Szenarien unten. Entspricht [deployment.md](deployment.md) §6 — *„rebuild and redistribute client hotfixes only when runtime controls cannot contain the issue“*.
2. **Functions-Redeploy** auf letzten bekannten guten Commit (`firebase deploy --only functions` bzw. selektiv) wenn Logik/Regression backend-seitig.
3. **Client-Hotfix** nur wenn 1+2 die Nutzerlast nicht fangen (z. B. harter App-Bug). Release-Prozess: [release-checklist.md](release-checklist.md).

---

## 1. Payment / Stripe / Webhook

**Typische Signale:** Stripe-Dashboard Fehler, Webhook-5xx in Cloud Logging, doppelte/fehlende Order-Bestätigungen, `stripeMerchWebhook` / hosted-checkout Pfad in [backend.md](backend.md) §6.

| Sofort | Details |
| --- | --- |
| Stückzahl stoppen (Commerce) | Checkout / neue Bestellungen über Runtime oder Owner-UI begrenzen, bis Ursache klar. Kein „weiterklicken“ bei Integrität unklar. |
| Secrets / Endpoint | `STRIPE_*` in Secret Manager prüfen; Webhook-Signaturfehler in Logs. Keine blinden Secret-Rotationen ohne Rollback-Plan. |
| Nachvollziehbarkeit | Betroffene `orders/*` und Zahlungsstatus in Firestore mit interner Spur abgleichen, nicht in Bulk „per Hand“ patchen. |

**Rollback:** Provider-seitig Webhook temporär pausieren (Stripe) *nur* wenn missbräuchliches Eingangssignal; ansonsten Runtime-Config: z. B. `userWritesEnabled` / betriebsintern dokumentierte Commerce-Sicherung — abgestimmt mit [deployment.md](deployment.md) §5 (kein leichtfertiges Live-Config-Experiment).

**Nachbereitung:** Post-Mortem mit Zeitlinie; [deployment.md](deployment.md) §7 *„checkout … webhook health if live“* erneut vollständig abhaken.

---

## 2. AI Cost Spike / Abuse Spike

**Signale:** GCP-Budget-Alerts, Anstieg Vertex/xAI-Ausgaben, `suspiciousSpike*` / Cost-Guard-Logs in Functions, ungewöhnliche `aiUsage*`-Muster.

| Sofort | Details |
| --- | --- |
| Kosten drosseln | `adminConfig`-gestützte AI-Runtime-Settings (Owner) — u. a. härtere Caps, Degrade-Modelle, `costGuard`/`globalDailyCaps`-Logik; siehe Code-Defaults in `functions/index.js` und [backend.md](backend.md). |
| Abuse | `appCheckMode` auf `monitor` nur in Abstimmung ([app-check-rollout.md](../app-check-rollout.md)); **nicht** „enforce“ wahllos deaktivieren. App Check bleibt Security-Thema. |
| Automatik | `budgetLockdownEnabled` / Pub/Sub `billing-budget-alerts` (siehe README/Backend) — verifizieren, ob `mergeRuntimeConfig` getriggert hat; [owner-admin.md](owner-admin.md) §6. |

**Rollback:** Konservativere Owner-Settings → bei massiver Explosion: globales KI-Limit über dokumentierte Admin-Pfade, nicht „still im Code“ ohne Review.

**Nachbereitung:** Ursache: Token-Flood, kompromittiertes Konto, fehlerhafte App-Version? ggf. Nutzersegment über Claims/Quoten.

---

## 3. Firebase Functions — Fehler / Timeouts

**Signale:** Steigende 5xx auf HTTPS/Callable, `timeout`/`memory` in Logs, `us-central1`-Engpass beobachtet.

| Sofort | Details |
| --- | --- |
| Triage | Kritische Exporte: `generateAi*`, `skydownAgent`, Webhooks, `authorizeAiUsage` — in Logs nach `logger.error` / Stack filtern. |
| Containment | Wiederholungs-Stürme: rate-limit/Client-seitig (siehe App-Check + Retry-Logik); ggf. betroffene Callables per **selektivem** Redeploy einer stabilen Version (siehe [deployment.md](deployment.md) §3). |
| Kapazität | V2-Optionen (`timeoutSeconds`, `maxInstances` wo im Code gesetzt) — **keine** wahllose Erhöhung bei Kosten ohne Owner-Freigabe. |

**Rollback:** Letzter guter `firebase deploy --only functions[:name]`-Stand; unmittelbar danach §7 in [deployment.md](deployment.md) (Auth, AI callable, Webhooks) smoke-testen.

---

## 4. Auth / Login

**Signale:** Massen-`PERMISSION_DENIED`, `syncCurrentUserClaims`/`setUserRole` Fehlschläge, App-Check-Reject-Welle, Provider-Ausfall (Google).

| Sofort | Details |
| --- | --- |
| Unterscheiden | Firebase Auth-Status (Konsole) vs. App-Check-Token (Monitor-Phase Logs) vs. `runtimeConfig.lockdown`. |
| `lockdown` | Wenn fälschlich aktiv: Owner **gezielt** anpassen (`setRuntimeLockdown` / `system/runtimeConfig` — nichts außerhalb des Modells). |
| Registrierung | `registrationsEnabled: false` nur bei Abuse; Owner-Bypass-Regeln beachten (Bootstrap-Owner). |

**Rollback:** App-Check nicht dauerhaft `off` für Convenience; stattdessen [app-check-rollout.md](../app-check-rollout.md) und Ursache (Token, Uhrzeit, Drift) beheben.

---

## 5. App-Release-Regression

**Signale:** Crash-Spike in Stores, Feature-Regression nach spezifischer Build-Nummer, A/B nicht vorhanden — harte Trennung Last vs. Client-Bug.

| Sofort | Details |
| --- | --- |
| Wo | Repro auf **Store-Build**, nicht nur lokal; [release-checklist.md](release-checklist.md) Smoke erneut fokussiert. |
| Stoppen | Gestufter Rollout zurückdrehen (Play) / TestFlight-Scope (iOS) — Prozess siehe [release/*](release/) wie im Repo verlinkt. |
| Backend-First? | Wenn Fehler in Callable/Config: Server-Fix; wenn nur Client: kein Backend-„Hack“, der Trust bricht. |

**Rollback:** Store-Version; parallel Runtime-Config nur für servergestützte Effekte (siehe §6), nicht als Ersatz für einen kaputten Client-Build, wenn der Fix nur im Binary liegt.

---

## 6. Globaler Lockdown / Runtime-Config-Notfall

**Wann:** SEV1, unklare Exfiltration, massiver Write-Abuse, Zwang zur sofortigen Schadensbegrenzung — [owner-admin.md](owner-admin.md) §6.

| Feld (Auszug) | Typische Wirkung |
| --- | --- |
| `lockdown` | Breite Schreib-/Zugriffssperre für Nicht-Owner (mit Ausnahme owner paths). |
| `userWritesEnabled` | Granulares Runterfahren userseitiger Writes. |
| `uploadsEnabled` / `registrationsEnabled` | Media/Onboarding begrenzen. |
| `budgetLockdownEnabled` | Kosten-/Budget-Schutz. |
| `appCheckMode` | `monitor` / `enforce` — nur im Einverständnis mit Security. |
| `lastLockdownReason` | **Immer** setzen (Nachvollziehbarkeit, Post-Mortem). |

**Ablauf:** Owner nutzt `setRuntimeLockdown` bzw. kontrolliertes `mergeRuntimeConfig` (kein wildes Überschreiben ohne `updatedBy`-/Audit-Spur in eurem Betrieb). Nach Stabilisierung: **stufenweise** zurückdrehen (Uploads → Writes → Registrierungen), jeweils mit Rauchtest gemäß [deployment.md](deployment.md) §7.

**Rollback:** Nicht „alles false“ in einem Rutsch, wenn Ihr dabei versehentlich Schutz abnehmt — priorisiert nach Risiko.

---

## Nachbereitung (PIE)

- **Post-Mortem** (SEV1/2, max. 1–2 Seiten): Zeitlinie, Root-Cause (Hypothese+Evidence), was lief gut, Action Items mit Owner+Deadline.
- **Dokumentation:** Änderung an [deployment.md](deployment.md), [backend.md](backend.md) oder Prozess nur, wenn Fakten/Commands dauerhaft anders.
- **Monitoring-Follow-up:** Wenn derselbe Alert wieder feuert, ist das Runbook nicht „fertig“ — Schwellwert/Signal prüfen.

---

*Version: an Repo-Zustand gebunden. Bei Abweichung echter Produkt-IDs, Kanäle, Owner-Konten immer das **interne Betriebshandbuch** und die Firebase-GUI als Quelle of truth; dieses Runbook fasst die **SkyOS/Skydown-Mechaniken** im Repo zusammen.*
