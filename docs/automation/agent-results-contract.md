# Agent Results Contract (SkyOS)

Dieses Dokument definiert das empfohlene JSON-Format fuer Agent-/Workflow-Outputs in SkyOS.

## Ziel

Externe Workflows (Activepieces/n8n) sollen Antworten liefern, die direkt in SkyOS-UI, History und Run-Status funktionieren.

## Top-Level Response (empfohlen)

```json
{
  "message": "Kurztext fuer Chat-Bubble",
  "status": "queued|running|completed|failed",
  "workflowStatus": "queued|running|completed|failed",
  "results": []
}
```

Hinweis:

- `message` wird fuer Chat-/Workflow-Summary genutzt.
- `workflowStatus` wird bevorzugt ausgewertet.
- Falls `workflowStatus` fehlt, wird auf `status`/`state` gemappt.

## `results[]` Grundschema

Jeder Eintrag ist ein Objekt mit mindestens:

- `type` (required)
- und den zugehoerigen Typfeldern (siehe unten)

Allgemein akzeptierte Felder:

- `id` (optional)
- `type` (required)
- `title` (optional)
- `text` (optional)
- `url` (optional)
- `mimeType` (optional)
- `fileName` (optional)
- `html` (optional)
- `columns` (optional, table)
- `rows` (optional, table)
- `workflowName` (optional)
- `status` (optional, workflow)
- `summary` (optional, workflow)
- `runId` (optional, workflow)

## Unterstuetzte Typen

### 1) `text`

```json
{
  "type": "text",
  "text": "Zusammenfassung oder Ergebnistext."
}
```

### 2) `workflow`

```json
{
  "type": "workflow",
  "workflowName": "SkyOS Master Workflow",
  "status": "queued",
  "summary": "Workflow wurde in die Warteschlange gestellt.",
  "runId": "abc123"
}
```

### 3) `link`

```json
{
  "type": "link",
  "title": "Dashboard",
  "text": "Link oeffnen",
  "url": "https://example.com/path"
}
```

Wichtig: SkyOS oeffnet nur sichere URLs. Nutzt immer `https://`.

### 4) `file`

```json
{
  "type": "file",
  "title": "Report",
  "fileName": "report.pdf",
  "mimeType": "application/pdf",
  "url": "https://example.com/report.pdf"
}
```

### 5) `image`

```json
{
  "type": "image",
  "title": "Generated Image",
  "url": "https://example.com/image.jpg"
}
```

### 6) `video`

```json
{
  "type": "video",
  "title": "Teaser",
  "url": "https://example.com/video.mp4"
}
```

### 7) `audio`

```json
{
  "type": "audio",
  "title": "Voiceover",
  "url": "https://example.com/audio.mp3"
}
```

### 8) `table`

```json
{
  "type": "table",
  "title": "Asset Plan",
  "columns": ["Asset", "Owner", "Status"],
  "rows": [
    ["Cover", "Design", "done"],
    ["Snippet", "Video", "running"]
  ]
}
```

### 9) `html`

```json
{
  "type": "html",
  "title": "Preview",
  "html": "<p><b>Release</b> fuer Freitag vorbereitet.</p>"
}
```

### 10) `reminder`, `task`, `note`

Diese Typen sind fuer live angelegte Productivity-Eintraege gedacht. Sie erlauben der App,
Workflow-Ergebnisse lesbar zusammenzufassen und direkt als erstellte Eintraege zu zaehlen.

Reminder:

```json
{
  "type": "reminder",
  "id": "reminderDocumentId",
  "title": "Arzttermin",
  "text": "Reminder erstellt: Arzttermin",
  "scheduledAt": "2026-05-04T09:00:00.000Z"
}
```

Task:

```json
{
  "type": "task",
  "id": "taskDocumentId",
  "title": "Steuer Unterlagen hochladen",
  "text": "Task erstellt: Steuer Unterlagen hochladen",
  "dueAt": "2026-05-02T12:00:00.000Z"
}
```

Note:

```json
{
  "type": "note",
  "id": "noteDocumentId",
  "title": "Meeting Notiz",
  "text": "Notiz erstellt: Meeting Notiz"
}
```

Hinweis: Script-Tags werden clientseitig entfernt.

## Feldprioritaeten im Backend

SkyOS liest Messages in dieser Reihenfolge:

1. `reply`
2. `message`
3. `status`
4. `result`

Results werden gelesen aus:

1. `results`
2. `outputs`
3. `assets`
4. `files`

## Begrenzungen (Server-Normalisierung)

- Maximal `12` Result-Eintraege.
- Tabellen:
  - bis `8` Spalten
  - bis `12` Zeilen
- Text-/URL/HTML-Felder werden serverseitig gekuerzt.

## Best Practices fuer Workflows

- Liefere immer `message`.
- Setze fuer lange Runs `workflowStatus = queued|running`.
- Liefere bei finalem Ende `workflowStatus = completed|failed`.
- Nutze `results` fuer strukturierte Inhalte statt alles in `message`.
- Bei Links/Medien nur `https`-URLs nutzen.

## Komplettes Multi-Asset Beispiel

```json
{
  "message": "Assets vorbereitet und bereit zur Freigabe.",
  "workflowStatus": "completed",
  "results": [
    {
      "type": "workflow",
      "workflowName": "SkyOS Master Workflow",
      "status": "completed",
      "summary": "Alle Schritte erfolgreich abgeschlossen.",
      "runId": "run_42"
    },
    {
      "type": "text",
      "text": "Hier sind deine finalen Assets."
    },
    {
      "type": "image",
      "title": "Cover",
      "url": "https://cdn.example.com/cover.jpg"
    },
    {
      "type": "video",
      "title": "Promo Clip",
      "url": "https://cdn.example.com/promo.mp4"
    },
    {
      "type": "file",
      "title": "Copy Paket",
      "fileName": "copy.txt",
      "mimeType": "text/plain",
      "url": "https://cdn.example.com/copy.txt"
    },
    {
      "type": "link",
      "title": "Freigabe Board",
      "url": "https://app.example.com/board/abc"
    }
  ]
}
```
