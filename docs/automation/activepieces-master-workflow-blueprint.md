# Activepieces Master Workflow Blueprint (SkyOS)

Dieses Blueprint-Dokument ist auf die aktuelle SkyOS-Implementierung abgestimmt (`functions/index.js`, iOS/Android Agent-Clients, `agentRunStatusCallback`).

## 1) Zielbild

Ein zentraler Activepieces-Master-Workflow soll **alle sinnvollen Agent-Automation-Requests** verarbeiten, robust routen und in einem einheitlichen SkyOS-Response-Format antworten.

Architektur:

`Webhook In -> Validate -> Intent Detection -> Router -> Action Subflows -> Callback Status Updates -> Response Composer -> Webhook Out`

## 2) Eingangsdaten (wie sie SkyOS heute sendet)

SkyOS sendet an den externen Workflow ein JSON mit:

```json
{
  "provider": "activepieces",
  "scope": "owner_global|user_personal",
  "requestId": "1714382030000_ab12cd34",
  "workflowName": "SkyOS Owner Activepieces Flow",
  "trigger": "agent_release|agent_briefing|agent_content|agent_merch|agent_automation",
  "source": "agent",
  "timestamp": "2026-04-27T10:16:00.000Z",
  "data": {
    "mode": "release|briefing|content|merch|automation",
    "prompt": "User Prompt",
    "reply": "Agent Reply",
    "history": [
      {"role": "user", "text": "..."},
      {"role": "assistant", "text": "..."}
    ],
    "automationScope": "owner|personal",
    "attachments": [
      {
        "id": "optional-stable-id",
        "name": "brief.pdf",
        "kind": "document|image|video|audio|text|file",
        "mimeType": "application/pdf",
        "source": "inline",
        "inlineBase64": "..."
      }
    ],
    "knowledgeContext": "...",
    "sourceRole": "agent_gateway"
  },
  "knowledgeContext": "...",
  "user": {
    "uid": "optional_if_enabled",
    "email": "optional_if_enabled",
    "role": "optional_if_enabled"
  }
}
```

Hinweis: `user` ist nur enthalten, wenn `sendsUserContext=true`.

## 3) Pflichtanforderungen fuer den Master-Workflow

- Idempotency auf `requestId` (mindestens dedup innerhalb kurzer Zeitfenster)
- Fruehes Status-Feedback an SkyOS (`queued`, dann `running`)
- Finaler Abschlussstatus (`completed` oder `failed`)
- Antwort kompatibel zum SkyOS Result Contract
- Keine unsicheren Links (`http`, `javascript:`) erzeugen

## 4) Empfohlene Node-Struktur in Activepieces

### A. Webhook Trigger

- Endpoint empfaengt SkyOS Payload.
- Persistiere Input roh in Debug-Log (gekürzt, keine sensitiven Tokens).

### B. Normalize & Validate

Pflichtfelder:
- `requestId`
- `data.mode`
- `data.prompt` oder `data.reply`

Fallback-Regeln:
- `mode = "automation"` wenn leer
- `prompt = ""` wenn nicht vorhanden
- `history = []` wenn nicht vorhanden

Wenn ungueltig:
- direkt `failed` callback senden
- kurze Fehlerantwort zurueckgeben

### C. Callback `queued` (sofort)

POST auf:

`https://us-central1-<PROJECT_ID>.cloudfunctions.net/agentRunStatusCallback`

Header:
- `Content-Type: application/json`
- `x-skyos-run-callback-secret: <AGENT_RUN_CALLBACK_SECRET>`

Body:

```json
{
  "requestId": "{{requestId}}",
  "uid": "{{user.uid}}",
  "runId": "{{data.agentRunId}}",
  "state": "queued",
  "workflowName": "SkyOS Master Workflow",
  "message": "Workflow wurde in die Warteschlange gestellt.",
  "provider": "activepieces"
}
```

### D. Intent Detection

Inputs fuer Intent:
- `data.mode`
- `data.prompt`
- `data.reply`
- `history` tail

Intent-Beispiele:
- `release_plan`
- `briefing_package`
- `content_pipeline`
- `merch_launch`
- `ops_automation`
- `generic_assist`

### E. Router

Regelvorschlag:
- `mode=release` -> Release Subflow
- `mode=briefing` -> Briefing Subflow
- `mode=content` -> Content Subflow
- `mode=merch` -> Merch Subflow
- `mode=automation` -> Intent-basiert auf Subflows
- sonst -> Generic Subflow

### F. Callback `running` (bei Start echter Verarbeitung)

Body:

```json
{
  "requestId": "{{requestId}}",
  "uid": "{{user.uid}}",
  "runId": "{{data.agentRunId}}",
  "state": "running",
  "workflowName": "SkyOS Master Workflow",
  "message": "Workflow wird ausgefuehrt.",
  "provider": "activepieces"
}
```

### G. Action Subflows

Jeder Subflow liefert standardisierte Outputs:
- `summaryText`
- `links[]`
- `assets[]` (optional)
- `files[]` (optional)
- `table` (optional)

### H. Response Composer (SkyOS-kompatibel)

Finales Response-JSON:

```json
{
  "message": "Kurztext fuer die Chat-Bubble",
  "workflowStatus": "completed",
  "results": [
    {
      "type": "workflow",
      "workflowName": "SkyOS Master Workflow",
      "status": "completed",
      "summary": "Alle Schritte erfolgreich abgeschlossen.",
      "runId": "optional"
    },
    {
      "type": "text",
      "text": "Konkretes Ergebnis in 1-3 Saetzen."
    },
    {
      "type": "link",
      "title": "Board",
      "url": "https://example.com/board"
    },
    {
      "type": "file",
      "title": "Report",
      "fileName": "report.pdf",
      "mimeType": "application/pdf",
      "url": "https://example.com/report.pdf"
    }
  ]
}
```

### I. Callback final `completed|failed`

Erfolg:

```json
{
  "requestId": "{{requestId}}",
  "uid": "{{user.uid}}",
  "runId": "{{data.agentRunId}}",
  "state": "completed",
  "workflowName": "SkyOS Master Workflow",
  "message": "Workflow erfolgreich abgeschlossen.",
  "provider": "activepieces"
}
```

Fehler:

```json
{
  "requestId": "{{requestId}}",
  "uid": "{{user.uid}}",
  "runId": "{{data.agentRunId}}",
  "state": "failed",
  "workflowName": "SkyOS Master Workflow",
  "message": "Workflow fehlgeschlagen: <kurzer Grund>",
  "provider": "activepieces"
}
```

## 5) SkyOS-kompatible Result-Typen (sicher)

- `text`
- `workflow`
- `link` (nur `https`)
- `file`
- `image`
- `video`
- `audio`
- `table`
- `html`

Nicht ueberfrachten: max 12 Result-Eintraege empfehlen.

## 6) Fehlerstrategie

### Retrybare Fehler
- API timeout
- 429 / 5xx von externen Diensten

Aktion:
- Subflow-intern Retry mit begrenztem Backoff
- Status bleibt `running`

### Nicht-retrybare Fehler
- Validation fehlgeschlagen
- Pflichtdaten fehlen
- Sicherheitsverstoss (ungueltige URL)

Aktion:
- Sofort `failed` callback
- response `message` kurz, technisch verwertbar

## 7) Sicherheitsbaseline fuer den Workflow

- Nur `https://` in allen `url`-Feldern
- Keine Secrets in `results` oder `message`
- Callback Secret nur in Header, nie im Body
- Bei user context: minimal noetige Felder verarbeiten

## 8) Minimaler Rollout-Plan

1. Master-Webhook + Normalize + queued/running/completed callback
2. Einen stabilen Router-Pfad (z. B. `content`)
3. SkyOS-konformes Response Composer Template
4. Fehlerpfad (`failed`) sauber testen
5. Weitere Subflows schrittweise anschliessen

## 9) Testfaelle (Pflicht)

- `mode=content` mit gueltigem Prompt -> `completed`, `results` > 0
- Leerer Prompt -> `failed` mit klarer Message
- Nur `requestId` vorhanden (ohne `uid/runId`) -> callback muss trotzdem aufloesbar sein
- Link-Output mit `http://` -> im Workflow auf `https` korrigieren oder verwerfen
- Langer Lauf > 20s -> `queued` und `running` sichtbar, dann final

## 10) Definition of Done

- SkyOS zeigt Workflow-Card mit sinnvollem Status-Text
- Strukturierte Results werden in Chat angezeigt (mind. `text` + `link` + `file` getestet)
- History speichert `resultType`, `automationMessage`, `workflowName`, `agentRunId`, `results`
- Kein unsicherer Link wird ausgegeben
- Fehler sind fuer User kurz und fuer Ops nachvollziehbar
