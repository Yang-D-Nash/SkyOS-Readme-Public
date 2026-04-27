# Activepieces: Minimaler, app-tauglicher Flow (SkyOS)

Damit **Automation in der App** wirklich funktioniert, muss der externe Webhook so antworten, wie es `skydownAgent` in den Cloud Functions erwartet: **HTTP 200** und ein **JSON-Body** (siehe [agent-results-contract.md](agent-results-contract.md)). Ein leerer Trigger oder ein Flow ohne Antwort reicht nicht.

## 1) Zwei haeufige Fehler

1. **Nur Catch Webhook, kein „Return Response“**  
   SkyOS sendet `POST` und liest **die Antwort** aus. Ohne **Return Response**-Schritt (Webhook-Piece) kommt kein gueltiger Contract an die App.

2. **Falsche Webhook-URL in SkyOS**  
   Fuer eine **synchrone** Antwort nutzt Activepieces typisch eine URL mit Suffix **`/sync`** (in der Webhook-Doku / im Trigger angezeigt). In den SkyOS-Automation-Einstellungen muss **`webhookPath`** genau der **Pfad** sein, den Activepieces fuer **synchron** anzeigt (z. B. `…/v1/webhooks/…/sync` — exakt aus der UI kopieren, nicht raten).

   Technisch: `baseURL` + `webhookPath` werden serverseitig zu einer URL verbunden (`buildAutomationWebhookUrl` in `functions/index.js`).

## 2) Minimaler Flow in Activepieces (manuell)

1. **Trigger:** „Catch Webhook“ (nicht „Empty“).  
   - Authentication: **None**, sofern ihr in SkyOS keinen zusaetzlichen Header abgleicht.  
2. (Optional) **Code** o. a.: Payload pruefen; Zugriff auf eingehende Daten: meist `{{trigger.body}}` bzw. im Test die Struktur aus [01-skyos-inbound-webhook.example.json](json-samples/reference-http-payloads/01-skyos-inbound-webhook.example.json).  
3. **Action:** im gleichen Stueck **Webhook** → **Return Response**.  
4. **Body (JSON),** z. B.:

```json
{
  "message": "Kurz fuer die App.",
  "workflowStatus": "completed",
  "results": [
    { "type": "text", "text": "Optionale Details." }
  ]
}
```

5. **Publish.**  
6. In Activepieces die **fertige Webhook-URL (sync)** kopieren und in **SkyOS** als `webhookPath` (nur der Pfad **oder** laut eurer Doku) bzw. zusammen mit `baseURL` eintragen.  
7. In der App: Agent → Automation aktivieren und senden.

## 3) Laengere Laeufe

Wenn etwas **laenger** dauert: `workflowStatus` auf `queued` / `running` setzen **und** optional Status per [agentRunStatusCallback](agent-run-callback-playbook.md) senden. Die **synchrone** Antwort muss trotzdem in der **Timeout**-Frist von SkyOS/Activepieces zurueckkommen (sonst 408/Netzfehler im Backend).

## 4) Nicht verwechseln

- **Dieser** Webhook = **Agent-Automation** (JSON wie im [Master-Blueprint](activepieces-master-workflow-blueprint.md)).  
- **Reminder / Task / Note** = andere Endpoints ([workflow-http-api-activepieces.md](../workflow-http-api-activepieces.md)), andere Secrets — eigenes Thema.

Kurz: **Catch Webhook → (optional Logik) → Return Response mit JSON-Contract → sync-URL in SkyOS eintragen** — dann funktioniert es mit der App.
