# ChatGPT-Prompt: Activepieces-Workflows für SkyOS

Den **Kasten unten** komplett in ChatGPT einfügen (Sprache des Modells egal). Anschließend musst du nur noch **Platzhalter** ersetzen (`PROJECT_ID`, Secrets, ggf. Stück-Versionen), die ChatGPT nicht kennen kann.

---

## Prompt zum Kopieren

```
Du bist ein Experte für Activepieces (cloud.activepieces.com) und für API-Integrationen.

Kontext SkyOS (WICHTIG – Priorität):
- Für „User gibt Text/Datei ein und erwartet sofort Antwort in der App“ reicht EIN synchroner Master-Flow. Ablauf: User → Firebase Callable skydownAgent → HTTP POST JSON an Activepieces Webhook-URL mit /sync → Activepieces: Return Response mit JSON → App zeigt Output.
- Die App ruft Activepieces nicht direkt an; nur das Backend sendet an den Webhook.
- Eingehend: u.a. data.prompt, data.reply, data.history, knowledgeContext, ggf. data.attachments (Metadaten; keine echten Dateibytes in AP außer SkyOS liefert sinnvoll im JSON). Ausgehend: message, workflowStatus, results[] gemäß Agent-Results-Contract. Für type "text" Feld "text" (oder message/summary/description) — nicht "content". file/link nur mit https-URLs.
- Mindestens 80% deiner Antwort: Flow (1) „SkyOS – Agent Webhook (Master)“.

Aufgabe:

(Schwerpunkt) (1) „SkyOS – Agent Webhook (Master)“
   - Trigger: Catch Webhook, Auth None, synchrone /sync-URL.
   - Optional: Code/Router nach data.mode; Input aus trigger body.
   - Abschluss: Return Response mit vollständigem Beispiel-JSON inkl. message, workflowStatus completed, results mit text- und file-Eintrag (https-Beispiel-URL).
   - Erkläre baseURL vs. webhookPath für SkyOS-Admin.
   - Kurz: Activepieces verarbeitet Anhänge nur über im JSON mitgelieferte Infos, nicht magisch.

(Kurz, optional) (2) Run-Status Callbacks — NUR für lange asynchrone Jobs, nicht für direkte Eine-Antwort: HTTP POST an https://us-central1-<PROJECT_ID>.cloudfunctions.net/agentRunStatusCallback, Header x-skyos-run-callback-secret, Body mit requestId/state, PLATZHALTER.

(Kurz, optional) (3) Workflow HTTP Task-Beispiel — NUR wenn ZUSÄTZLICH ein Task in Firebase aus demselben Szenario angelegt werden soll: createTaskFromWorkflow, Header x-skyos-workflow-secret, PLATZHALTER — nicht derselbe Weg wie Agent-Webhook.

Lieferform:
- Detailliert nummeriert nur für (1). Für (2)(3) je 5–8 Sätze.
- Checkliste „Publish & SkyOS testen“.
- Keine erfundenen Secrets: PROJECT_ID und Geheimnisse als PLATZHALTER.
```

---

## Was du danach selbst einsetzen musst

| Platzhalter | Woher |
|-------------|--------|
| `PROJECT_ID` | Firebase-Konsole → Projekteinstellungen |
| `AGENT_RUN_CALLBACK_SECRET` | Wie in `docs/automation/agent-run-callback-playbook.md` (Firebase Secret) |
| `SKYOS_WORKFLOW_SECRET` / Header `x-skyos-workflow-secret` | Wie in `docs/workflow-http-api-activepieces.md` |
| Webhook-URL mit `/sync` | Aus dem Catch-Webhook-Trigger in **deiner** Activepieces-Instanz kopieren |

Fachlich abgleichen mit: [activepieces-master-workflow-blueprint](activepieces-master-workflow-blueprint.md), [agent-results-contract](agent-results-contract.md), [activepieces-minimal-app-flow](activepieces-minimal-app-flow.md).
