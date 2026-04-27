# Agent Run Callback Playbook (Activepieces / n8n)

Dieser Guide beschreibt die konkrete Rueckmeldung von Workflow-Status an SkyOS.

## Ziel

SkyOS erwartet Status-Updates fuer Agent-Runs ueber den HTTP-Endpoint:

- `POST https://us-central1-<PROJECT_ID>.cloudfunctions.net/agentRunStatusCallback`

Der Endpoint aktualisiert `users/{uid}/agentRuns/{runId}` und wird durch App-Polling angezeigt.

## Auth Header

Der Callback braucht den Secret-Header:

- Header-Name: `x-skyos-run-callback-secret`
- Header-Wert: Inhalt von `AGENT_RUN_CALLBACK_SECRET`

Ohne gueltigen Header kommt `401 unauthorized`.

## Minimal Payload

Mindestens einer der beiden Schluessel muss aufloesbar sein:

- `uid + runId` (bevorzugt)
- oder `requestId` (Fallback)

### Empfohlenes Standard-Payload

```json
{
  "uid": "USER_UID",
  "runId": "AGENT_RUN_ID",
  "requestId": "1714382030000_ab12cd34",
  "state": "queued",
  "workflowName": "SkyOS Master Workflow",
  "message": "Workflow wurde in die Warteschlange gestellt.",
  "provider": "activepieces",
  "progressPercent": 10,
  "step": "Input validieren",
  "etaSeconds": 45,
  "details": "Dateien werden vorbereitet."
}
```

## Erlaubte Statuswerte

Der Backend-Mapping normalisiert auf:

- `queued`
- `running`
- `completed`
- `failed`

Synonyme wie `pending`, `in_progress`, `success`, `error` werden intern gemappt.

## Empfohlene Callback-Schritte im Workflow

1. **Queued**
   - Direkt nach Trigger-Eingang callbacken.
2. **Running**
   - Sobald erste echte Verarbeitung startet.
3. **Completed**
   - Wenn alle Actions erfolgreich fertig sind.
4. **Failed**
   - Bei finalem Fehler mit kurzer Message.

## Activepieces HTTP-Action (Beispiel)

- **Method:** `POST`
- **URL:** `https://us-central1-<PROJECT_ID>.cloudfunctions.net/agentRunStatusCallback`
- **Headers:**
  - `Content-Type: application/json`
  - `x-skyos-run-callback-secret: {{YOUR_CALLBACK_SECRET}}`
- **Body (JSON):**

```json
{
  "uid": "{{trigger.body.user.uid}}",
  "runId": "{{trigger.body.data.agentRunId}}",
  "requestId": "{{trigger.body.requestId}}",
  "state": "running",
  "workflowName": "{{trigger.body.workflowName}}",
  "message": "Assets werden generiert.",
  "provider": "activepieces",
  "progressPercent": 60,
  "step": "Assets rendern",
  "etaSeconds": 20
}
```

Hinweis: Wenn `runId` im externen Flow nicht vorhanden ist, `requestId` trotzdem mitschicken. SkyOS kann dann per `automationRequestId` aufloesen.

## n8n HTTP Request Node (Beispiel)

- **HTTP Method:** `POST`
- **URL:** `https://us-central1-<PROJECT_ID>.cloudfunctions.net/agentRunStatusCallback`
- **Send Headers:** `true`
  - `x-skyos-run-callback-secret`: `<SECRET>`
  - `Content-Type`: `application/json`
- **Send Body:** `JSON`

```json
{
  "uid": "={{$json.user.uid}}",
  "runId": "={{$json.data.agentRunId}}",
  "requestId": "={{$json.requestId}}",
  "state": "completed",
  "workflowName": "={{$json.workflowName}}",
  "message": "Workflow erfolgreich abgeschlossen.",
  "provider": "n8n",
  "progressPercent": 100,
  "step": "Fertig"
}
```

## Troubleshooting

- `400 uid_runId_or_requestId_required`
  - Weder (`uid` + `runId`) noch `requestId` geliefert.
- `401 unauthorized`
  - Callback-Secret Header fehlt/falsch.
- `404 run_not_found`
  - Run konnte nicht aufgeloest werden (falsche IDs, falsches Projekt, zu frueher Callback).
- `500 internal_error`
  - Backend-Fehler, Logs in Cloud Functions pruefen.

## Best Practice fuer Master-Workflow

- Sende frueh `queued`.
- Sende bei long-running Tasks `running`.
- Sende immer final `completed` oder `failed`.
- Halte `message` kurz (UI-geeignet, 1 Satz).
- Optional fuer bessere UX: `progressPercent`, `step`, `etaSeconds`, `details`.
