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
  "scheduledAt": "2026-05-04T09:00:00.000Z",
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

## Premium Router Code Step

Der aktuelle Drop-in-Code fuer `SkyOS - Agent Webhook Master PRO` liegt hier:

[activepieces-router-premium-v2.5.mjs](activepieces-router-premium-v2.5.mjs)

Einsatz:

- In Activepieces einen **Code** Step anlegen.
- Den Inhalt der Datei in den Step kopieren.
- `projectId` und `workflowSecret` als Activepieces Inputs/Secrets setzen.
- Danach die Code-Ausgabe im **Return Response** Step als JSON zurueckgeben.

v2.5 ergaenzt gegenueber v2.4:

- direkte Modi `reminder`, `task`, `note` zusaetzlich zu `release`, `briefing`, `content`, `merch`, `social_analysis`
- strukturierte App-Results mit `type: "reminder"`, `type: "task"` oder `type: "note"`
- lesbare Success-Messages fuer den User statt generischem `Erledigt. Aktion erfolgreich ausgefuehrt.`
- `social_analysis` schreibt die echte Agent-Auswertung plus Social-Handles und vorhandene Live-Kontexte als Note, nicht nur einen generischen Platzhalter
- `private` und `group` bleiben fuer Founder Briefings unveraendert, damit Home `Me/Group` bzw. `Ich/Gruppe` sauber anzeigen kann
- der Secret Header `x-skyos-workflow-secret` wird nur beim Server-Call gesetzt und nie in der Response ausgegeben

Founder Briefings triggern serverseitig vor dem Lesen der KPI-Daten einen frischen Sync fuer den
angefragten Berichtstag. Kostenwerte werden nur angezeigt, wenn echte/reconciled
`totalActualCostMicros` vorliegen. `totalEstimatedCostMicros` wird bewusst nicht als Euro-Wert
ausgegeben.

Instagram Social Analysis nutzt Meta Graph API nur, wenn ein gueltiger, nicht abgelaufener
`META_IG_USER_ACCESS_TOKEN` plus `META_IG_USER_ID` fuer ein verbundenes Business-/Creator-Konto
vorliegt. Wenn Meta den Token ablehnt, arbeitet SkyOS mit Handle + Prompt weiter und markiert die
Live-Graph-Daten als nicht verfuegbar.

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
