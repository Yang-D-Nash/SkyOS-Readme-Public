# SkyOS AI-Anleitung für Nutzer und Entwickler

Diese Anleitung erklärt die AI-Flächen in SkyOS so, dass normale Nutzer wissen, was sie tun
können und Entwickler verstehen, was im Backend passiert. Sie ist bewusst produktnah geschrieben:
keine Marketing-Sprache, keine versteckten Annahmen.

## 1. Kurzfassung

SkyOS AI besteht aus drei sichtbaren Hauptbereichen:

| Bereich | Für Nutzer | Technisch |
| --- | --- | --- |
| AI-Bot | schnelle Antworten, Copy, Ideen, FAQ-Hilfe, einfache Visuals | `generateAiText` und `generateAiVisual` |
| Agent | strukturierte Aufgaben, Briefings, Pläne, Workflows, Automation | `skydownAgent`, optional externe Workflows |
| Owner AI-Steuerung | Prompt-, Runtime-, Limit-, FAQ- und Provider-Steuerung | Firestore `adminConfig/*` plus Cloud Functions |

Wichtig: Der Client fragt AI nicht direkt beim Provider an. Kritische Entscheidungen laufen über
Firebase Cloud Functions, damit Limits, Rollen, Planstatus, App Check, Provider-Routing,
Kill-Switches und Kostenkontrollen serverseitig greifen.

## 2. Was Nutzer mit SkyOS AI machen können

Nutzer können AI für kreative und operative Arbeit nutzen:

- Texte, Hooks, Captions, Claims, Ideen und Entwürfe erstellen
- schnelle Fragen zur App, Membership, AI-Limits oder Nutzung stellen
- Visuals aus Prompts erzeugen, wenn Visual Generation freigeschaltet ist
- im Agent längere Aufgaben strukturieren lassen, z. B. Release-Plan, Briefing, Content-Paket,
  Merch-Drop, To-do-Liste oder Kampagnenstruktur
- Agent-Ergebnisse als Text, Link, Datei, Bild, Video, Audio, Tabelle oder Workflow-Status erhalten,
  sofern der Backend- oder Workflow-Output diese Result-Typen liefert
- bei verbundenen Workflows Tasks, Notes oder Reminder anlegen lassen, wenn die Automation aktiv,
  erlaubt und korrekt konfiguriert ist
- den eigenen Verlauf im Rahmen der planabhängigen Aufbewahrung wiederfinden

Gute Nutzer-Erwartung:

- AI ist ein Assistent, kein Ersatz für eigene Prüfung.
- Antworten können falsch, unvollständig oder zu allgemein sein.
- Membership und Rollen bestimmen, wie viel und wie tief AI genutzt werden kann.
- Bei Limits, Sperren, Runtime-Lockdown oder Provider-Ausfall kann AI eine Anfrage ablehnen oder
  kürzer beantworten.
- Store-Käufe, Restore und Entitlement-Sync können kurze Zeit brauchen.
- Externe Automationen sind nur aktiv, wenn sie im Backend/Owner-Bereich eingerichtet und erlaubt sind.

## 3. Was Nutzer nicht erwarten sollten

SkyOS AI soll nicht:

- verbindliche Rechts-, Steuer-, Medizin-, Finanz- oder Sicherheitsberatung geben
- Passwörter, API Keys, Zahlungsdaten oder andere hochsensible Geheimnisse verarbeiten müssen
- Versand-, Preis-, Membership-, Order- oder Support-Regeln erfinden, die nicht im System stehen
- Owner-, Commerce- oder Account-Aktionen heimlich ausführen
- externe Workflows ohne erlaubte Route, Secret, Rolle, Plan und Runtime-Konfiguration starten
- garantieren, dass ein generierter Text oder ein Bild rechtlich, markenrechtlich oder kommerziell
  ohne Review nutzbar ist

Nutzer sollten AI-Ausgaben vor Veröffentlichung, Kaufentscheidung, Versand an Dritte oder
Weiterverarbeitung prüfen.

## 4. Nutzerfluss in der App

### AI-Bot

Der Bot ist für schnelle Aufgaben gedacht.

Typische Prompts:

- `Schreib mir 5 Hook-Ideen für ein Skydown Reel.`
- `Mach daraus eine kurze Caption mit premium underground Vibe.`
- `Gib mir drei Claims für einen Hoodie Drop.`
- `Warum sehe ich mein AI-Limit?`

Der Client sendet an `generateAiText`:

- `prompt`
- `mode`
- `aiLevel`

Die Antwort enthält u. a.:

- `reply`
- `historyRetentionDays`
- optional `usage`
- optional `botDecision`

### Visual Generation

Visuals sind für Key-Visuals, Moodbilder und kreative Entwürfe gedacht. Der Client sendet an
`generateAiVisual`:

- `prompt`
- `aiLevel`

Die Antwort enthält:

- kurze Beschreibung
- `imageBase64`
- `mimeType`
- `historyRetentionDays`
- optional `usage`
- optional `botDecision`

Bei temporären Provider- oder Serverfehlern versucht der Android-Client Visual Generation einmal
erneut.

### Agent

Der Agent ist für strukturierte, längere oder workflow-nahe Arbeit gedacht.

Typische Prompts:

- `Plane einen 7-Tage-Content-Run für den nächsten Drop.`
- `Schreib ein Briefing für ein Musikvideo mit Shotlist.`
- `Mach daraus Tasks und Reminder für diese Woche.`
- `Analysiere diesen Release-Plan und gib nächste Schritte.`

Der Client sendet an `skydownAgent`:

- `prompt`
- Chat-`history`
- `mode`
- `aiLevel`
- `executeAutomation`
- `automationScope`
- optional `attachments`
- optional `socialSetup`
- optional `idempotencyKey`
- optional Manus BYOS Key, wenn diese Route erlaubt ist

Die Antwort kann enthalten:

- `reply`
- `automationTriggered`
- `automationAttempted`
- `automationMessage`
- `workflowName`
- `agentProvider`
- `providerFallbackUsed`
- `agentRunId`
- `resultType`
- strukturierte `results`
- `usage`
- `agentDecision`

Für längere Runs kann die App mit `getAgentRunStatus` den Status nachladen.

## 5. Backend-Verständnis für Entwickler

### Vertrauensmodell

AI ist Teil des Backend-Vertrauensmodells. Der Client ist nur UI und Transport. Das Backend muss
entscheiden:

- ist der Nutzer angemeldet?
- ist App Check gültig oder in einem erlaubten Soft-Fail?
- welche Rolle und welcher Quota-Plan gelten?
- ist AI global, für diese Rolle oder für diese Anfrage erlaubt?
- ist das Tageslimit erreicht?
- muss die Anfrage blockiert, gekürzt, auf einen günstigeren Pfad geroutet oder mit Upgrade-Hinweis
  beantwortet werden?
- welcher Provider darf genutzt werden?
- darf eine externe Automation gestartet werden?
- ist eine Bestätigung für Commerce oder Owner Ops erforderlich?

### Wichtige Backend-Funktionen

| Funktion | Aufgabe |
| --- | --- |
| `authorizeAiUsage` | Nutzung vorab prüfen und erlauben/blockieren |
| `reconcileAiUsageCost` | tatsächliche Nutzung/Kosten nachziehen |
| `generateAiText` | Bot-Textantworten erzeugen |
| `generateAiVisual` | Visuals erzeugen |
| `skydownAgent` | Agent-Antworten, Provider-Routing und optionale Workflows |
| `getAgentRunStatus` | Status längerer Agent-/Workflow-Runs lesen |
| `getAiFaqOwnerIntelligence` | FAQ-Qualität und Owner-Review-Loop |
| `previewAiFaqReviewRecommendation` | FAQ-Änderung vorab ansehen |
| `applyAiFaqReviewRecommendation` | FAQ-Empfehlung anwenden |
| `revertLastAiFaqReviewChange` | letzte FAQ-Änderung zurückrollen |
| `syncAndroidAiSubscriptionStatus` / `syncIosAiSubscriptionStatus` | Store-Entitlements mit Nutzer-Plan synchronisieren |

### Zentrale Firestore-Dokumente

| Pfad | Bedeutung |
| --- | --- |
| `users/{uid}` | Rolle, Quota-Plan, AI-Limits, Consent, Subscription-Status, Retention |
| `adminConfig/aiPromptSettings` | Bot-, Visual-, Agent- und FAQ-Anweisungen |
| `adminConfig/aiRuntime` | Provider, Fallback, Grenzwerte, Sicherheitsregeln, Kostenwächter, Agent-Kern |
| `adminConfig/ownerActivepiecesFlow` | owner-verwaltetes Activepieces Gateway |
| `adminConfig/automationN8n_<uid>` | optionale nutzereigene Workflow-Konfiguration |
| `system/runtimeConfig` | Lockdown, Uploads, Registrierungen, App Check, Budget-Kontrollen |
| `agentExternalBridgeAudit/*` | Audit Trail für externe Agent-Bridges |

### Quota- und Planlogik

Default-Limits liegen serverseitig in `functions/src/security/roles.js`.

| Plan | Text/Tag | Visuals/Tag | Agent/Tag | Verlauf |
| --- | ---: | ---: | ---: | ---: |
| `free` | 30 | 4 | 18 | 3 Tage |
| `creator` | 120 | 20 | 70 | 7 Tage |
| `studio` | 240 | 40 | 140 | 30 Tage |
| `internal_team` | 240 | 40 | 140 | 30 Tage |
| `owner_unlimited` | 5000 | 1200 | 3000 | 30 Tage |

Diese Werte können durch Nutzer-Dokumente, Rollen-Sync, Subscription-Sync und Runtime-Caps begrenzt
oder ergänzt werden. Die UI darf Limits anzeigen, aber nicht als Autorität behandeln.

### Runtime-Steuerung

`adminConfig/aiRuntime` steuert u. a.:

- `agentProvider` und `fallbackAgentProvider`
- harte Tages-Caps pro Nutzer/Kind
- globale Tages-Caps
- Manus-Einstellungen
- Knowledge-/Google-Drive-Optionen
- Bot-Modellpolitik für Text und Visuals
- Kostenwächter und kurze Antworten bei kritischem Budget
- FAQ-/Guide-Routing
- Fallback-Policy
- Sicherheitsregeln
- Agent-Core: erlaubte Tasks, blockierte Tasks, Tools, externe Provider, Confirmation-Policy,
  Kill Switch, Timeout, Retry

`system/runtimeConfig` bleibt die höherliegende Plattformkontrolle für Lockdown, App Check,
Uploads, Registrierungen, Nutzer Writes und Budget-Lockdown.

### Prompt-Einstellungen

`adminConfig/aiPromptSettings` enthält:

- `textInstruction`
- `visualInstruction`
- `agentSystemInstruction`
- `faqInstruction`
- `faqKnowledgeBase`
- `assetLibraryLink`
- `assetReferenceNotes`

Entwickler sollten neue AI-Flächen nicht mit hartcodierten Produktversprechen bauen. Wenn ein
Fakt dynamisch, legal relevant, planabhängig oder owner-gesteuert ist, gehört er in Live Facts,
Owner Knowledge oder Runtime/Prompt-Einstellungen.

## 6. Externe Automation

Der Agent kann externe Workflows nutzen, aber nur backend-gesteuert.

Unterstützte Muster:

- owner-verwalteter Activepieces Flow
- optional nutzereigene Activepieces/n8n-Konfiguration
- optional Manus BYOS
- HTTP-Workflow-API für `createReminderFromWorkflow`, `createTaskFromWorkflow`,
  `createNoteFromWorkflow`

Regeln:

- keine direkten Secrets im Client
- kein direkter Firestore-Schreibzugriff aus Activepieces
- HTTP-Workflow-API nur mit `x-skyos-workflow-secret`
- JSON-Body muss die Ziel-`uid` enthalten
- Workflows sollen idempotent arbeiten, wenn `requestId` oder `idempotencyKey` vorhanden ist
- für strukturierte Agent-Antworten das Format aus `docs/automation/agent-results-contract.md`
  verwenden

## 7. Sicherheit, Datenschutz und Kommunikation

Nutzerseitige Texte sollten ehrlich bleiben:

- AI hilft beim Entwerfen, Strukturieren und Automatisieren.
- AI kann Fehler machen.
- Limits schützen Kosten, Fairness und Plattformstabilität.
- Externe Workflows sind optional und können je nach Setup fehlen.
- Bei sensiblen Aktionen wird geblockt oder eine Bestätigung verlangt.
- Keine hochsensiblen Secrets in Prompts.

Technisch sollten Entwickler darauf achten:

- Auth und App Check nicht clientseitig umgehen
- Firestore/Storage Rules aktualisieren, wenn neue Collections entstehen
- Owner/Admin-Flächen nie nur über UI verstecken
- neue AI-Funktionen immer mit Quota, Audit, Retention und Abuse-Fällen denken
- Provider-Fehler als normale Betriebsrealität behandeln
- Fallbacks, Retry und Nutzer-Erklärung sauber trennen

## 8. Entwickler-Checkliste für neue AI-Funktionen

Vor Merge oder Release prüfen:

- Gibt es eine Cloud Function oder bewusst serverseitige Route?
- Wird Auth geprüft?
- Wird App Check berücksichtigt?
- Wird Rolle/Plan/Quota geprüft?
- Gibt es ein klares nutzerseitiges Limit- oder Fehlerverhalten?
- Wird Nutzung abgeglichen oder anderweitig auditierbar gemacht?
- Sind Runtime-Kill-Switches oder Owner-Steuerung vorhanden, wenn das Feature Kosten oder Risiko hat?
- Sind Prompt-Anweisungen in `aiPromptSettings` oder Runtime-Konfiguration statt hart im Client?
- Sind rechtliche und professionelle Beratungsgrenzen sichtbar?
- Sind externe Automationen geheimnisfrei, idempotent und serverseitig abgesichert?
- Sind relevante Dokumente aktualisiert?

## 9. Relevante Dokumente

- [ai-system.md](ai-system.md) - AI-Architektur und Systemgrenzen
- [backend.md](backend.md) - Firebase, Functions, Rules, Runtime-Steuerung
- [USER_GUIDE.md](USER_GUIDE.md) - allgemeine Nutzer-Anleitung
- [owner-admin.md](owner-admin.md) - Owner-Steuerung und Betrieb
- [workflow-http-api-activepieces.md](workflow-http-api-activepieces.md) - Activepieces HTTP API
- [automation/agent-results-contract.md](automation/agent-results-contract.md) - Agent-Result-Format
- [legal/AI_USAGE_NOTICE.md](legal/AI_USAGE_NOTICE.md) - AI Usage Notice
