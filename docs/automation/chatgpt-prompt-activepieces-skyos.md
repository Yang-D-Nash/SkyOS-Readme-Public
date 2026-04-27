# ChatGPT-Prompt: Activepieces-Workflows für SkyOS

Den **Kasten unten** komplett in ChatGPT einfügen (Sprache des Modells egal). Anschließend musst du nur noch **Platzhalter** ersetzen (`PROJECT_ID`, Secrets, ggf. Stück-Versionen), die ChatGPT nicht kennen kann.

---

## Prompt zum Kopieren

```
Du bist ein Experte für Activepieces (cloud.activepieces.com) und für API-Integrationen.

Kontext SkyOS:
- Die App ruft NICHT selbst Activepieces auf. Das Firebase-Backend (Callable skydownAgent) sendet bei aktivierter Automation einen HTTP POST an eine konfigurierte Webhook-URL mit JSON-Body (Agent-Automation).
- Der Webhook muss dem Client im GLEICHEN Request antworten (synchrone Ausführung): Dafür in Activepieces die Webhook-URL mit Suffix /sync verwenden (wie in der AP-Doku „Synchronous Requests“), sofern die Plattform das so anzeigt. Im Return-Response-Schritt JSON zurückgeben.
- Die Antwort-JSON muss zum „Agent Results Contract“ passen: mindestens message, workflowStatus (queued|running|completed|failed), optional results[] mit erlaubten types (text, workflow, link, file, image, video, audio, table, html). Nur https-Links. Max. ca. 12 Result-Einträge.
- Eingehendes JSON (vereinfacht) enthält u.a.: requestId, trigger (z.B. agent_release), data.mode (release|briefing|content|merch|automation), data.prompt, data.reply, data.history, optional data.user aus user-Objekt, knowledgeContext.

Aufgabe:
Erstelle mir genau DREI getrennte Activepieces-Workflow-Konzepte als klare Schritt-für-Schritt-Bauanleitungen (Trigger → Aktionen → Publish), jeweils mit sinnvollem deutschen/englischen Anzeigenamen:

(1) „SkyOS – Agent Webhook (Master)“
   - Trigger: Webhook / Catch Webhook, Auth None (sofern kein Header-Abgleich nötig).
   - Optional: Code-Schritt, der requestId und data.mode aus dem Body liest und validiert.
   - Router oder Verzweigung nach data.mode mit kurzen Beispiel-Antworttexten (ohne externe APIs).
   - Abschluss: Aktion „Return Response“ (Webhook-Piece) mit Beispiel-JSON-Body, der den Contract erfüllt.
   - Kurz erklären, welche URL-Teile in SkyOS als baseURL bzw. webhookPath einzutragen sind (nur der Pfad inkl. /sync, exakt aus der AP-UI kopieren).

(2) „SkyOS – Run-Status Callbacks (optional)“
   - Für längere Läufe: HTTP-Request(s) an POST https://us-central1-<PROJECT_ID>.cloudfunctions.net/agentRunStatusCallback
   - Header: Content-Type application/json, x-skyos-run-callback-secret: <PLATZHALTER>
   - Body-Felder: mindestens requestId, state (queued|running|completed|failed), optional uid, runId, message, provider activepieces
   - Erkläre, dass PROJECT_ID und Secret aus dem eigenen Firebase-Projekt stammen (Platzhalter lassen).

(3) „SkyOS – Workflow HTTP (Task-Beispiel)“
   - GETRENNT von der Agent-Automation: Server-zu-Server POST an https://us-central1-<PROJECT_ID>.cloudfunctions.net/createTaskFromWorkflow
   - Header: x-skyos-workflow-secret: <PLATZHALTER>, Content-Type application/json
   - Beispiel-Body mit uid, title, source activepieces, requestId
   - Hinweis: Das ist die Task/Note/Reminder-API, nicht der Agent-Webhook.

Lieferform:
- Pro Flow: nummerierte Schritte für die Activepieces-UI, welches Piece, welche Felder.
- Pro Flow: ein validierbares Beispiel-JSON für Return Response bzw. Request-Body.
- Eine kurze Checkliste „Nach dem Publish in SkyOS testen“.
- Keine erfundenen Geheimnisse: überall PLATZHALTER für Secrets und PROJECT_ID.
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
