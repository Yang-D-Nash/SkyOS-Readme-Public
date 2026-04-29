# SkyOS AI-Anleitung

Diese Anleitung erklärt SkyOS AI aus zwei Perspektiven:

- **Für Nutzer:** Was kann ich mit AI, Bot und Agent machen, was darf ich erwarten und wo liegen die Grenzen?
- **Für Entwickler:** Welche Backend-Flüsse, Rollen, Limits, Runtime-Einstellungen und Sicherheitsregeln müssen respektiert werden?

Beide Perspektiven stehen bewusst in einem Dokument. So bleibt klar, dass das sichtbare
Nutzererlebnis und der technische Backend-Vertrag zusammengehören.

## 1. Produktversprechen

SkyOS AI ist kein einzelner Chat-Button. Sie ist ein kontrolliertes Assistenzsystem für kreative,
produktive und workflow-nahe Arbeit innerhalb von SkyOS.

Nutzer sollen AI verwenden können, um schneller zu schreiben, Ideen zu entwickeln, Visuals zu
entwerfen, Briefings vorzubereiten, Aufgaben zu strukturieren und Workflows anzustoßen. Gleichzeitig
muss klar bleiben: AI entscheidet nicht heimlich über Käufe, Rollen, Owner-Aktionen, rechtliche
Fragen oder sensible Automationen.

Das Backend bleibt die Autorität. Der Client zeigt die Oberfläche, aber Limits, Rollen,
Memberships, Provider-Routing, App Check, Kill-Switches und externe Automationen werden serverseitig
kontrolliert.

## 2. Die AI-Bereiche

| Bereich | Nutzererlebnis | Backend-Vertrag |
| --- | --- | --- |
| AI-Bot | schnelle Antworten, Copy, Hooks, Ideen, FAQ-Hilfe | `generateAiText` |
| Visual Generation | Key-Visuals, Moodbilder, kreative Bildentwürfe | `generateAiVisual` |
| Agent | strukturierte Aufgaben, Briefings, Pläne, Workflows | `skydownAgent` |
| Agent-Status | Fortschritt längerer Läufe nachvollziehen | `getAgentRunStatus` |
| Owner AI-Steuerung | Prompts, Limits, Provider, FAQ und Runtime verwalten | Firestore `adminConfig/*` plus Cloud Functions |

## 3. Anleitung für Nutzer

### Was Nutzer machen können

SkyOS AI eignet sich für:

- Captions, Hooks, Claims, Post-Ideen und kurze Copy
- kreative Konzepte für Musik, Video, Merch und Kampagnen
- einfache App-, Membership-, Order- oder Support-Fragen
- Visuals aus Text-Prompts, wenn Visual Generation aktiv ist
- strukturierte Agent-Aufgaben wie Release-Plan, Briefing, Shotlist, Content-Paket oder To-do-Liste
- workflow-nahe Ergebnisse wie Tasks, Notes oder Reminder, wenn Automation freigeschaltet und korrekt eingerichtet ist
- strukturierte Agent-Ergebnisse wie Text, Links, Dateien, Bilder, Videos, Audio, Tabellen oder Workflow-Status

### Gute Prompts

Gute Prompts sind konkret und beschreiben Ergebnis, Kontext und Ton:

- `Schreib mir 5 Hook-Ideen für ein Skydown Reel, kurz, roh, premium, nicht generisch.`
- `Mach daraus eine Caption für Instagram mit 3 Varianten und einem klaren CTA.`
- `Plane einen 7-Tage-Content-Run für den nächsten Drop mit täglichen Aufgaben.`
- `Schreib ein Musikvideo-Briefing mit Ziel, Stimmung, Shotlist, Locations und Deliverables.`
- `Erstelle ein Key-Visual für einen dunklen urbanen Hoodie Drop, cinematic, hochwertig, wenig Text.`

### Was Nutzer erwarten dürfen

Nutzer dürfen erwarten:

- klare Antworten statt unnötiger Theorie
- sichtbare Hinweise, wenn Limits, Membership oder Runtime-Regeln greifen
- verständliche Fehler, wenn Netzwerk, Provider oder Backend gerade nicht antworten
- keine heimlichen Commerce-, Owner- oder Account-Aktionen
- planabhängige Nutzung und planabhängige Verlaufsspeicherung
- bessere Ergebnisse, wenn Kontext und gewünschtes Format klar genannt werden

### Was Nutzer nicht erwarten sollten

SkyOS AI ist keine Garantie für Wahrheit, Rechtssicherheit oder professionelle Beratung.

AI soll nicht:

- verbindliche Rechts-, Steuer-, Medizin-, Finanz- oder Sicherheitsberatung geben
- Passwörter, API-Keys, private Zahlungsdaten oder andere hochsensible Geheimnisse benötigen
- Versand-, Preis-, Membership-, Order- oder Support-Regeln erfinden
- Owner-, Commerce- oder Account-Aktionen ohne erlaubte Route und Bestätigung ausführen
- externe Workflows starten, wenn Rolle, Plan, Runtime, Secret oder Konfiguration fehlen
- garantieren, dass ein generierter Text oder ein Bild ohne menschlichen Review veröffentlicht werden kann

Nutzer sollten AI-Ausgaben vor Veröffentlichung, Bestellung, Weitergabe an Dritte oder geschäftlicher
Verwendung prüfen.

## 4. Nutzerfluss in der App

### AI-Bot

Der AI-Bot ist für schnelle Unterstützung gedacht: Copy, Ideen, FAQ-Hilfe und kurze produktive
Antworten.

Der Client sendet an `generateAiText`:

- `prompt`
- `mode`
- `aiLevel`

Die Antwort kann enthalten:

- `reply`
- `historyRetentionDays`
- `usage`
- `botDecision`

### Visual Generation

Visual Generation ist für kreative Bildentwürfe gedacht. Der Client sendet an `generateAiVisual`:

- `prompt`
- `aiLevel`

Die Antwort enthält:

- kurze Beschreibung
- `imageBase64`
- `mimeType`
- `historyRetentionDays`
- optional `usage`
- optional `botDecision`

Bei temporären Provider- oder Serverfehlern kann der Android-Client einen zweiten Versuch ausführen.

### Agent

Der Agent ist für Aufgaben gedacht, die Struktur, Planung oder Workflow-Nähe brauchen.

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

Für längere Läufe kann die App mit `getAgentRunStatus` den Status nachladen.

## 5. Anleitung für Entwickler

### Grundregel

AI-Funktionen dürfen nicht nur im Client abgesichert werden. Jede Funktion, die Kosten verursacht,
Nutzerdaten verarbeitet, externe Provider anspricht, Workflows startet oder Berechtigungen betrifft,
braucht eine serverseitige Prüfung.

Das Backend entscheidet:

- ist der Nutzer angemeldet?
- ist App Check gültig oder in einem erlaubten Soft-Fail?
- welche Rolle und welcher Quota-Plan gelten?
- ist die AI-Fläche global und für diesen Nutzer erlaubt?
- ist das Tageslimit erreicht?
- muss die Anfrage blockiert, gekürzt, günstiger geroutet oder mit Upgrade-Hinweis beantwortet werden?
- welcher Provider darf genutzt werden?
- darf eine externe Automation gestartet werden?
- ist eine Bestätigung für Commerce oder Owner Ops erforderlich?

### Wichtige Backend-Funktionen

| Funktion | Aufgabe |
| --- | --- |
| `authorizeAiUsage` | Nutzung vorab prüfen und erlauben oder blockieren |
| `reconcileAiUsageCost` | tatsächliche Nutzung und Kosten nachziehen |
| `generateAiText` | Bot-Textantworten erzeugen |
| `generateAiVisual` | Visuals erzeugen |
| `skydownAgent` | Agent-Antworten, Provider-Routing und optionale Workflows |
| `getAgentRunStatus` | Status längerer Agent- oder Workflow-Läufe lesen |
| `getAiFaqOwnerIntelligence` | FAQ-Qualität und Owner-Review-Loop auswerten |
| `previewAiFaqReviewRecommendation` | FAQ-Änderung vorab ansehen |
| `applyAiFaqReviewRecommendation` | FAQ-Empfehlung anwenden |
| `revertLastAiFaqReviewChange` | letzte FAQ-Änderung zurückrollen |
| `syncAndroidAiSubscriptionStatus` / `syncIosAiSubscriptionStatus` | Store-Entitlements mit Nutzer-Plan synchronisieren |

### Zentrale Firestore-Dokumente

| Pfad | Bedeutung |
| --- | --- |
| `users/{uid}` | Rolle, Quota-Plan, AI-Limits, Consent, Subscription-Status, Verlaufsspeicherung |
| `adminConfig/aiPromptSettings` | Bot-, Visual-, Agent- und FAQ-Anweisungen |
| `adminConfig/aiRuntime` | Provider, Fallback, Grenzwerte, Sicherheitsregeln, Kostenwächter, Agent-Kern |
| `adminConfig/ownerActivepiecesFlow` | owner-verwaltetes Activepieces Gateway |
| `adminConfig/automationN8n_<uid>` | optionale nutzereigene Workflow-Konfiguration |
| `system/runtimeConfig` | Lockdown, Registrierungen, User Writes, App Check, Budget-Kontrollen |
| `agentExternalBridgeAudit/*` | Audit Trail für externe Agent-Bridges |

## 6. Limits, Pläne und Verlauf

Default-Limits liegen serverseitig in `functions/src/security/roles.js`.

| Plan | Text pro Tag | Visuals pro Tag | Agent pro Tag | Verlauf |
| --- | ---: | ---: | ---: | ---: |
| `free` | 30 | 4 | 18 | 3 Tage |
| `creator` | 120 | 20 | 70 | 7 Tage |
| `studio` | 240 | 40 | 140 | 30 Tage |
| `internal_team` | 240 | 40 | 140 | 30 Tage |
| `owner_unlimited` | 5000 | 1200 | 3000 | 30 Tage |

Diese Werte sind Defaults. Effektive Limits können durch Nutzer-Dokumente, Rollen-Sync,
Subscription-Sync, harte Runtime-Caps und globale Caps weiter begrenzt oder ergänzt werden.

Die UI darf Limits anzeigen. Sie darf aber nie die letzte Autorität über Nutzung, Rollen oder
Entitlements sein.

## 7. Runtime- und Prompt-Steuerung

`adminConfig/aiRuntime` steuert unter anderem:

- primären Agent-Provider und Fallback-Provider
- harte Tages-Caps pro Nutzer und AI-Art
- globale Tages-Caps
- Manus-Einstellungen
- Knowledge- und Google-Drive-Optionen
- Modellpolitik für Text und Visuals
- Kostenwächter und kurze Antworten bei kritischem Budget
- FAQ- und Guide-Routing
- Fallback-Verhalten
- Sicherheitsregeln
- erlaubte Tasks, blockierte Tasks, Tools, externe Provider, Bestätigungspflichten, Kill Switch, Timeout und Retry

`adminConfig/aiPromptSettings` steuert:

- `textInstruction`
- `visualInstruction`
- `agentSystemInstruction`
- `faqInstruction`
- `faqKnowledgeBase`
- `assetLibraryLink`
- `assetReferenceNotes`

Entwickler sollten dynamische, planabhängige, rechtlich relevante oder owner-gesteuerte Fakten nicht
hart im Client einbauen. Solche Informationen gehören in Live Facts, Owner Knowledge,
Runtime-Einstellungen oder Prompt-Einstellungen.

## 8. Externe Automation

Der Agent kann externe Workflows nutzen, aber nur serverseitig kontrolliert.

Unterstützte Muster:

- owner-verwalteter Activepieces Flow
- optionale nutzereigene Activepieces- oder n8n-Konfiguration
- optional Manus BYOS
- HTTP-Workflow-API für `createReminderFromWorkflow`, `createTaskFromWorkflow` und `createNoteFromWorkflow`

Regeln:

- keine Secrets im Client
- kein direkter Firestore-Schreibzugriff aus Activepieces
- HTTP-Workflow-API nur mit `x-skyos-workflow-secret`
- JSON-Body muss die Ziel-`uid` enthalten
- Workflows sollen idempotent arbeiten, wenn `requestId` oder `idempotencyKey` vorhanden ist
- strukturierte Agent-Antworten sollen dem Vertrag in [automation/agent-results-contract.md](automation/agent-results-contract.md) folgen

## 9. Sicherheits- und Kommunikationsstandard

Nutzerseitige Texte sollen ehrlich, ruhig und konkret sein:

- AI hilft beim Entwerfen, Strukturieren und Automatisieren.
- AI kann Fehler machen.
- Limits schützen Kosten, Fairness und Plattformstabilität.
- Membership kann mehr Reichweite und tiefere Workflows ermöglichen.
- Externe Workflows sind optional und hängen vom Setup ab.
- Bei sensiblen Aktionen wird blockiert oder eine Bestätigung verlangt.
- Hochsensible Secrets gehören nicht in Prompts.

Technisch gilt:

- Auth und App Check nicht umgehen
- Owner/Admin-Flächen nie nur über UI verstecken
- Firestore- und Storage-Regeln aktualisieren, wenn neue Collections entstehen
- neue AI-Funktionen immer mit Quota, Audit, Retention, Missbrauch und Provider-Ausfall denken
- Fallback, Retry und Nutzer-Erklärung sauber trennen
- externe Automationen geheimnisfrei, idempotent und serverseitig abgesichert halten

## 10. Entwickler-Checkliste

Vor Merge oder Release prüfen:

- Gibt es eine serverseitige Route oder Cloud Function?
- Wird Auth geprüft?
- Wird App Check berücksichtigt?
- Wird Rolle, Plan und Quota geprüft?
- Gibt es ein klares nutzerseitiges Limit- oder Fehlerverhalten?
- Wird Nutzung abgeglichen oder anderweitig auditierbar gemacht?
- Gibt es Kill-Switches oder Owner-Steuerung, wenn Kosten oder Risiko entstehen?
- Sind Prompts und Runtime-Werte konfigurierbar, statt hart im Client zu liegen?
- Sind rechtliche und professionelle Beratungsgrenzen sichtbar?
- Sind externe Automationen idempotent und serverseitig abgesichert?
- Sind die passenden Dokumente aktualisiert?

## 11. Relevante Dokumente

- [ai-system.md](ai-system.md) - AI-Architektur und Systemgrenzen
- [backend.md](backend.md) - Firebase, Functions, Rules und Runtime-Steuerung
- [USER_GUIDE.md](USER_GUIDE.md) - allgemeine Nutzer-Anleitung
- [owner-admin.md](owner-admin.md) - Owner-Steuerung und Betrieb
- [workflow-http-api-activepieces.md](workflow-http-api-activepieces.md) - Activepieces HTTP API
- [automation/agent-results-contract.md](automation/agent-results-contract.md) - Agent-Result-Format
- [legal/AI_USAGE_NOTICE.md](legal/AI_USAGE_NOTICE.md) - Hinweis zur AI-Nutzung
