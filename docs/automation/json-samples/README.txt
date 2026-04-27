SkyOS / Activepieces – JSON in diesem Ordner

IMPORTIERBAR IN ACTIVEPIECES (nur diese eine Datei im Root):
  activepieces-import-skyos-webhook-starter.json
  → „Import Flow“ in Activepieces; minimaler Catch-Webhook.

WICHTIG – „Empty Trigger“ / „Select Trigger“ nach dem Import
  Das passiert oft in der Cloud: der Flow-Name kommt an, der Piece-Trigger nicht.
  Dann ist das kein totales Scheitern — 2 Minuten manuell:
  1) Im Builder auf den Trigger-Kasten klicken (oder „Select Trigger“).
  2) Unter „Highlights“ oder Suche: „Webhook“ / „Catch Webhook“ waehlen.
  3) Authentication: None (kein Key noetig).
  4) Rechts oben **Publish** — dann erscheint die Webhook-URL; die in SkyOS
     (Owner-Automation: baseURL + webhookPath) eintragen.
  Ohne Publish gibt es keine feste URL fuer SkyOS.

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
