# Activepieces: Minimaler, app-tauglicher Flow (SkyOS)

## Prioritaet: ein synchroner Master reicht fuer „Text/Datei rein – Antwort raus“

Fuer **direkte Antworten** in der App brauchst du im Wesentlichen **nur einen** Flow:

**SkyOS – Agent Webhook (Master)** = synchroner Input/Output: `skydownAgent` (Firebase) sendet JSON an den Activepieces-Webhook (**URL mit `/sync`**, siehe unten) → Activepieces liest u. a. `data.prompt`, `data.reply`, `data.history`, `knowledgeContext` (und ggf. Anhaenge-Metadaten) → baut **Antwort-JSON** → Return Response → SkyOS zeigt dem User das Ergebnis.

| Thema | Wann noetig? |
|--------|----------------|
| **Agent Webhook (Master)** | Immer, wenn der User **sofort** Text/Struktur/Links aus dem Flow will. |
| **Workflow HTTP** (Task/Note/Reminder) | Nur **zusaetzlich**, wenn aus derselben Logik **Firebase**-Tasks/Notes/Reminders per [workflow-http-api](../workflow-http-api-activepieces.md) angelegt werden sollen (anderes Secret, andere Endpoints). |
| **Run-Status Callbacks** | Spaeter / nur bei **langen** Jobs (Fortschritt in der App); **nicht** noetig fuers synchrone Eine-Antwort-Szenario. |

Damit **Automation in der App** technisch funktioniert, muss der Webhook so antworten, wie es `skydownAgent` erwartet: **HTTP 200** und ein **JSON-Body** (siehe [agent-results-contract.md](agent-results-contract.md)). Ein leerer Trigger oder ein Flow ohne **Return Response** reicht nicht.

### Dateien / Anhaenge

Activepieces **verarbeitet keine echten Binaerdateien automatisch**, nur weil der User etwas in SkyOS hochlaedt. SkyOS packt in den Webhook-Body **Metadaten** (z. B. aus `data.attachments`: Name, MIME, ggf. Referenzen — siehe Master-Blueprint). In **Return Response** koennen **Downloads** als `type: "file"` (oder Link) mit **nur `https`-URLs** stehen.

Fuer `type: "text"` wertet das Backend u. a. `text`, `message`, `summary`, `description` aus — **nicht** ein Feld `content` (besser `text` nutzen).

**Beispiel-Return (gueltig fuer SkyOS):**

```json
{
  "message": "Ich habe deine Eingabe verarbeitet.",
  "workflowStatus": "completed",
  "results": [
    {
      "type": "text",
      "title": "Antwort",
      "text": "Hier ist das Ergebnis zur Anfrage."
    },
    {
      "type": "file",
      "title": "Ausgabedatei",
      "url": "https://example.com/output.pdf",
      "fileName": "output.pdf",
      "mimeType": "application/pdf"
    }
  ]
}
```

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
    { "type": "text", "title": "Details", "text": "Optionale Details." }
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

## 5) Security-Hinweis zu Webhook-Zielen

SkyOS blockiert externe Workflow-Ziele, wenn sie nicht als `https://` URL erreichbar sind oder auf lokale/private Hosts zeigen (z. B. `localhost`, Loopback, RFC1918-Netze).

Optional kann im Runtime-Config (`adminConfig/aiRuntime`) eine explizite Host-Allowlist gesetzt werden:

- `bot.agentCore.externalPolicy.allowedAutomationWebhookHosts`

Wenn gesetzt, sind nur diese Hosts (inkl. Subdomains) fuer Agent-Workflow-Webhooks erlaubt.
