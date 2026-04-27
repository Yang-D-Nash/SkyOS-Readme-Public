SkyOS / Activepieces – JSON in diesem Ordner

IMPORTIERBAR IN ACTIVEPIECES (nur diese eine Datei im Root):
  activepieces-import-skyos-webhook-starter.json
  → „Import Flow“ in Activepieces; minimaler Catch-Webhook.

NICHT importieren (nur Referenz / Copy-Paste):
  reference-http-payloads/
    01  Eingehender Agent-Webhook-Body (SkyOS → AP)
    02  Antwort-Body AP → SkyOS (Result Contract)
    03–04  agentRunStatusCallback (queued / completed)
    05–07  Workflow-HTTP-API (Task / Reminder / Note) — Header x-skyos-workflow-secret

Deckt das „7 Dateien“ alles in der App ab?
  Nein im Sinne von „7 getrennte Activepieces-Flows“. Die 7 Dateien sind 7
  **Beispiel-Rollen** (HTTP), kein 1:1-Produktmodul.
  In der App: Agent-Modi (z. B. release … automation) laufen über **einen**
  Webhook; im Blueprint routest du intern nach data.mode — **ein** Master-Flow
  reicht typisch. Die drei Workflow-Endpoints (Task/Reminder/Note) sind **zusätzlich**
  zur Agent-Automation; die kannst du in AP als eigene HTTP-Schritte oder
  eigene Mini-Flows bauen — nicht zwingend 7 Cloud-Flows.

Einmal-Download: skyos-activepieces-json-samples.zip (Root + reference-http-payloads).
