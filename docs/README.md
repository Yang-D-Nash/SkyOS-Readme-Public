# SkyOS Dokumentation

Willkommen in der zentralen Produkt- und Systemdokumentation von SkyOS.

Dieser Ordner erklärt SkyOS so, dass ein neuer Nutzer, ein Entwickler, ein Reviewer, ein Partner
oder der Betrieb schnell versteht, was die Plattform kann, wie sie aufgebaut ist und welche Grenzen
bewusst gesetzt sind. Die Dokumentation soll nicht wie ein internes Notizlager wirken, sondern wie
ein verlässlicher Einstieg in ein echtes Produkt.

## Orientierung

SkyOS ist die technische und operative Grundlage der Skydown-App: native mobile Clients,
Firebase-Backend, AI-Assistenten, Agent-Automation, Creator-Medien, Commerce, Membership,
Rechtstexte und Release-Betrieb.

Die wichtigsten Prinzipien:

- Nutzer sollen verstehen, was sie tun können und was sie erwarten dürfen.
- Entwickler sollen erkennen, welche Backend-Flüsse, Rollen, Limits und Sicherheitsgrenzen gelten.
- Owner und Betrieb sollen wissen, wo Live-Konfiguration, Release-Checks und Notfallsteuerung liegen.
- Recht, Datenschutz, Billing und AI-Nutzung sollen nicht in stillen Annahmen versteckt sein.

## Schnellstart nach Ziel

| Ziel | Einstieg |
| --- | --- |
| Produkt und System verstehen | [architecture.md](architecture.md), [backend.md](backend.md), [ai-guide.md](ai-guide.md) |
| AI aus Nutzer- und Entwicklersicht verstehen | [ai-guide.md](ai-guide.md), [ai-system.md](ai-system.md), [legal/AI_USAGE_NOTICE.md](legal/AI_USAGE_NOTICE.md) |
| App lokal bauen oder prüfen | [ios.md](ios.md), [android.md](android.md), [ci.md](ci.md) |
| Backend, Rules und Functions betreiben | [backend.md](backend.md), [deployment.md](deployment.md), [incident-runbook.md](incident-runbook.md) |
| Release vorbereiten | [release/app-release-workflow.md](release/app-release-workflow.md), [release/store-upload-runbook.md](release/store-upload-runbook.md), [release-checklist.md](release-checklist.md) |
| Owner- und Admin-Steuerung verstehen | [owner-admin.md](owner-admin.md), [backend.md](backend.md), [commerce.md](commerce.md) |
| Store-Auftritt und Review vorbereiten | [store/README.md](store/README.md), [store/app-store.md](store/app-store.md), [store/google-play.md](store/google-play.md) |
| Transparenz, Nachbau und Verantwortlichkeiten prüfen | [transparency-reengineering.md](transparency-reengineering.md), [compliance/README.md](compliance/README.md) |

## Kernbereiche

- [USER_GUIDE.md](USER_GUIDE.md) - verständlicher Produktführer für App-Nutzung, Bereiche und Erwartungen
- [ai-guide.md](ai-guide.md) - Premium-Anleitung für AI, getrennt nach Nutzererlebnis und Entwickler-/Backend-Vertrag
- [ai-system.md](ai-system.md) - technische AI-Systemkarte mit Bot, Visuals, Agent, FAQ, Limits und Sicherheit
- [backend.md](backend.md) - Firebase, Functions, Firestore, Storage, App Check, Runtime-Kontrollen und Backend-Autorität
- [commerce.md](commerce.md) - Shop, Orders, Checkout, Versand, Payment-Rails und Memberships
- [owner-admin.md](owner-admin.md) - Owner-Abläufe, Admin-Berechtigungen, Kill-Switches und Governance
- [architecture.md](architecture.md) - Gesamtarchitektur und Produktzusammenhang
- [branding.md](branding.md) - Sprache, Produktnamen, Copy-Regeln und Markenlogik

## Plattform und Entwicklung

- [ios.md](ios.md) - iOS-Setup, Signing, Build und QA
- [android.md](android.md) - Android-Setup, Compose-Build-Flow und Gerätehinweise
- [ci.md](ci.md) - CI-Jobs, Pfadfilter, Quality Gates und Trigger-Verhalten
- [deployment.md](deployment.md) - Deploy-Ablauf, selektive Deploys, Rollback und Checks nach dem Deploy
- [incident-runbook.md](incident-runbook.md) - Incident-Szenarien, Rollen, Schweregrade, Kommunikation und Hotfix-Pfade
- [workflow-http-api-activepieces.md](workflow-http-api-activepieces.md) - serverseitige HTTP-Workflow-API für Reminder, Tasks und Notes
- [automation/agent-results-contract.md](automation/agent-results-contract.md) - erwartetes Agent-/Workflow-Result-Format
- [automation/activepieces-minimal-app-flow.md](automation/activepieces-minimal-app-flow.md) - minimaler Activepieces-Flow für Agent-Automation
- [automation/chatgpt-prompt-activepieces-skyos.md](automation/chatgpt-prompt-activepieces-skyos.md) - Prompt-Vorlage für Activepieces-Workflow-Entwürfe

## Release und Store

Stand 2026-04-29 ist SkyOS als `1.0.0` Release Candidate für den Productivity- und
Automation-Launch dokumentiert und in den Store-Konsolen für interne/TestFlight-Pfade hochgeladen.
Reminder mit Push, Tasks, Notes und Activepieces-Erstellungsendpunkte sind die live ausgewiesene
Workflow-Fläche. Längerfristige Memory-Funktionen und tiefere Follow-up-Automationen bleiben
bewusst als nächster Schritt markiert.

Der öffentliche Store-Rollout hängt weiterhin von Store-Konsole, Recht, URLs, Asset-Mapping und
echten Geräte-Smokes außerhalb des Repos ab.

- [release/app-release-workflow.md](release/app-release-workflow.md) - verbindlicher Schritt-für-Schritt-Release für iOS, Android und Backend
- [release/store-upload-runbook.md](release/store-upload-runbook.md) - Build-Identität, Upload-Status, Blocker, Hashes und nächste Console-Schritte
- [release-checklist.md](release-checklist.md) - Go/No-Go-Checkliste für Release Candidates
- [../manual-test-checklist.md](../manual-test-checklist.md) - Rollen- und Plattform-Smoke-Matrix
- [beta-distribution.md](beta-distribution.md) - Zugangspfad für externe Tester und kurzes Feedback-Skript
- [store/README.md](store/README.md) - Store-Präsenz, Screenshots und Review-Vorbereitung
- [store/screenshots.md](store/screenshots.md) - Screenshot-Story und Store-Bildlogik

## Recht, Vertrauen und Compliance

- [legal/terms.md](legal/terms.md)
- [legal/privacy.md](legal/privacy.md)
- [legal/imprint.md](legal/imprint.md)
- [legal/SUBSCRIPTION_TERMS.md](legal/SUBSCRIPTION_TERMS.md)
- [legal/AI_USAGE_NOTICE.md](legal/AI_USAGE_NOTICE.md)
- [LEGAL_OVERVIEW.md](LEGAL_OVERVIEW.md)
- [compliance/README.md](compliance/README.md)

Diese Dokumente sind Teil des Vertrauensmodells. Änderungen an AI, Billing, Rollen, Store-Copy,
Account-Löschung, Support, Commerce oder Datenverarbeitung sollten immer gegen diese Texte geprüft
werden.

## Ergänzende Referenzen

- [brand-architecture-and-launch-positioning.md](brand-architecture-and-launch-positioning.md) - SkyOS/Skydown-Hierarchie, Launch-Framing und Differenzierung
- [design/SKYOS_EXPERIENCE_PRINCIPLES.md](design/SKYOS_EXPERIENCE_PRINCIPLES.md) - Experience-Prinzipien
- [faq.md](faq.md) - operative Antworten für Support und Nutzerfragen
- [localization-terminology-glossary.md](localization-terminology-glossary.md) - Terminologie und Übersetzungslogik
- [localization-audit.md](localization-audit.md) - Lokalisierungsprüfung
- [localization-roadmap.md](localization-roadmap.md) - Lokalisierungsplanung
- [ai-membership-funnel-metrics.md](ai-membership-funnel-metrics.md) - AI-Membership-Metriken
- [ai-subscriptions-rollout-brief.md](ai-subscriptions-rollout-brief.md) - AI-Subscription-Rollout
- [store/review-prep.md](store/review-prep.md) - Store-Review-Vorbereitung

## Kompatibilität

Ältere Einstiegspunkte wie `DEVELOPER_GUIDE.md`, `OWNER_GUIDE.md`, `LEGAL_OVERVIEW.md` und
`RELEASE_CHECKLIST.md` bleiben im Repository, damit bestehende Verweise nicht brechen. Die
kanonischen Inhalte liegen in den thematisch sortierten Dokumenten oben.

## Qualitätsstandard

Jede Änderung, die Nutzervertrauen, Produktverhalten, Berechtigungen, Billing, AI, Rechtstexte,
Release-Prozess oder Backend-Autorität betrifft, sollte mit einer passenden Dokumentationsänderung
kommen.

SkyOS soll nicht von stillen Annahmen leben. Die Dokumentation ist Teil des Produkts.
