# Activepieces -> SkyOS Workflow HTTP API

Diese Endpoints sind fuer serverseitige Workflow-Calls gedacht (kein direkter Firestore-Zugriff).
Sie erstellen Daten unter `users/{uid}/...`; Activepieces muss deshalb die Firebase `uid` des
Zielkontos im JSON-Body mitsenden.

## Security

- Methode: `POST`
- Header: `x-skyos-workflow-secret: <SKYOS_WORKFLOW_SECRET>`
- Secret kommt aus Firebase Secret `SKYOS_WORKFLOW_SECRET` (oder Runtime-Env gleichnamig)
- Der Secret Header ist Pflicht. Requests ohne gueltigen Header erhalten `401 unauthorized`.
- Keine echten Secret-Werte in Activepieces Screenshots, Logs oder Repo-Dateien committen.

Firebase Secret setzen:

```bash
firebase functions:secrets:set SKYOS_WORKFLOW_SECRET
firebase deploy --only functions
```

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

## Erwartete Antworten

Erfolg:

```json
{
  "ok": true,
  "uid": "firebaseUserId",
  "reminderId": "generatedDocumentId"
}
```

Je nach Endpoint heisst die ID `reminderId`, `taskId` oder `noteId`. Bei **Tasks** kann zusaetzlich `deduplicated: true` zurueckkommen, wenn bereits eine offene Task mit gleichem normalisierten Titel existiert (Server merged Metadaten).

Fehler:

- `400 title_required` oder `400 invalid_argument` bei unvollstaendigen Bodies
- `401 unauthorized` bei fehlendem/falschem `x-skyos-workflow-secret`
- `405 method_not_allowed` bei Methoden ausser `POST`
- `500 workflow_secret_missing`, wenn das Secret nicht in Functions konfiguriert ist

## Live Status

- Reminder: erstellt `users/{uid}/reminders/{id}` mit `scheduledAt`, `timezone`, `status=scheduled`; die Scheduled Function `processDueReminders` versendet Push, sobald der Reminder faellig ist.
- Task: erstellt `users/{uid}/tasks/{id}` mit `status=open`, `priority`, optionalem `dueAt` und `source=activepieces`.
- Note: erstellt `users/{uid}/notes/{id}` mit Titel, Inhalt und `source=activepieces`.
- Memory und weitere Automationen sind bewusst `coming next` und nicht Teil dieser Live-HTTP-API.
