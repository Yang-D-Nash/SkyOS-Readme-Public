# Activepieces -> SkyOS Workflow HTTP API

Diese Endpoints sind fuer serverseitige Workflow-Calls gedacht (kein direkter Firestore-Zugriff).

## Security

- Methode: `POST`
- Header: `x-skyos-workflow-secret: <SKYOS_WORKFLOW_SECRET>`
- Secret kommt aus Firebase Secret `SKYOS_WORKFLOW_SECRET` (oder Runtime-Env gleichnamig)

## Endpoints

- `createReminderFromWorkflow`
- `createTaskFromWorkflow`
- `createNoteFromWorkflow`

Basis-URL:

`https://us-central1-<project-id>.cloudfunctions.net/<endpointName>`

## Beispiel Bodies

Reminder:

```json
{
  "uid": "firebaseUserId",
  "title": "Arzttermin",
  "body": "Unterlagen mitnehmen",
  "scheduledAt": "2026-04-29T09:00:00.000Z",
  "timezone": "Europe/Berlin",
  "source": "activepieces",
  "requestId": "wf-rem-001"
}
```

Task:

```json
{
  "uid": "firebaseUserId",
  "title": "Steuer Unterlagen hochladen",
  "description": "PDF bis Freitag ablegen",
  "dueAt": "2026-05-02T12:00:00.000Z",
  "priority": "high",
  "source": "activepieces",
  "requestId": "wf-task-001"
}
```

Note:

```json
{
  "uid": "firebaseUserId",
  "title": "Meeting Notiz",
  "content": "Kernaussagen und naechste Schritte.",
  "source": "activepieces",
  "requestId": "wf-note-001"
}
```

## Activepieces HTTP Step (pro Endpoint)

- Method: `POST`
- URL: `https://us-central1-<project-id>.cloudfunctions.net/<endpointName>`
- Headers:
  - `Content-Type: application/json`
  - `x-skyos-workflow-secret: {{SKYOS_WORKFLOW_SECRET}}`
- Body Type: JSON
- Body: je nach Endpoint die passende Struktur oben
