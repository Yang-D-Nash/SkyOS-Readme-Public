# SkyOS Dokumentation

Dieser Ordner ist die Arbeitsgrundlage für SkyOS als Produkt, Codebase und Betriebssystem für das
Team dahinter. Ziel ist einfach: Entwickler, Partner, Reviewer, Investoren, Support und Founder
sollen verstehen können, was SkyOS ist, wie es gebaut ist und wie es betrieben wird, ohne das
Repository rückwärts entschlüsseln zu müssen.

## Hier starten

- [architecture.md](architecture.md) - vollständige Produkt- und Systemkarte
- [transparency-reengineering.md](transparency-reengineering.md) - deutsches Transparenz- und Reengineering-Dossier: was öffentlich ist, welche eigenen Accounts nötig sind und was geheim bleiben muss
- [backend.md](backend.md) - Firebase, Functions, Regeln, Runtime-Steuerung und Backend-Autorität
- [ios.md](ios.md) - iOS-Setup, Signing, Starten und QA
- [android.md](android.md) - Android-Setup, Compose-Build-Flow und Fold-Testhinweise
- [ai-system.md](ai-system.md) - AI-Assistent, FAQ, Visual Generation, Agent Runtime und Sicherheit
- [ai-guide.md](ai-guide.md) - Deutsche AI-Anleitung für Nutzer und Entwickler: Möglichkeiten, Backend-Ablauf, Limits und Erwartungen
- [commerce.md](commerce.md) - Merch, Orders, Checkout, Versand, Payment-Rails und Memberships
- [owner-admin.md](owner-admin.md) - Owner-Workflows, Steuerung, Kill-Switches und Release-Governance
- [deployment.md](deployment.md) - Deploy-Ablauf, selektive Deploys, Rollback-Grundlagen und Checks nach dem Deploy
- [incident-runbook.md](incident-runbook.md) - Incident-Szenarien, Rollen, Schweregrade, Kommunikation, Rollback vs. Hotfix
- [ci.md](ci.md) - CI-Jobs, Pfadfilter, Quality Gates und Trigger-Verhalten
- [release-checklist.md](release-checklist.md) - Release-Readiness-Gate über Produkt, Recht, Analytics und Vertrauen
- [branding.md](branding.md) - Produktnamen, Tonalität, Assets und Copy-Regeln
- [brand-architecture-and-launch-positioning.md](brand-architecture-and-launch-positioning.md) - finale SkyOS/Skydown-Hierarchie, Launch-Hero, sichtbare vs. sekundäre Features, Apple/Notion-artiges Framing und Differenzierung
- [workflow-http-api-activepieces.md](workflow-http-api-activepieces.md) - Activepieces-HTTP-Setup für Reminder-, Task- und Note-Erstellung
- [automation/activepieces-minimal-app-flow.md](automation/activepieces-minimal-app-flow.md) - minimaler Activepieces-Flow für **Agent-Automation** mit Sync-URL, Return Response und App-Vertrag
- [automation/chatgpt-prompt-activepieces-skyos.md](automation/chatgpt-prompt-activepieces-skyos.md) - **Copy-Paste-Prompt** für ChatGPT, um drei Activepieces-Workflows zu entwerfen: Agent Webhook, Callbacks und Workflow API
- [faq.md](faq.md) - nutzerseitige Antworten für Betrieb, Support und typische Fragen
- [store/README.md](store/README.md) - App-Store- und Google-Play-Auftritt, Screenshots und Review-Vorbereitung

## Aktueller Release-Einstieg

Stand 2026-04-29 ist SkyOS als `1.0.0` Release Candidate für den Productivity- und
Automation-Launch dokumentiert und in den Store-Konsolen für interne/TestFlight-Pfade hochgeladen.
Reminder mit Push, Tasks, Notes und Activepieces-Erstellungsendpunkte sind die live ausgewiesene
Workflow-Fläche. Längerfristige Memory-Funktionen und tiefere Follow-up-Automationen bleiben bewusst
als nächster Schritt markiert. Der öffentliche Store-Rollout hängt weiterhin von Store-Konsole,
Recht, URLs, Asset-Mapping und echten Geräte-Smokes außerhalb des Repos ab.

- [release/app-release-workflow.md](release/app-release-workflow.md) - **Schritt-für-Schritt App-Store-Release** für iOS, Android und Backend; **diese Datei pflegen**, wenn sich der Release-Prozess ändert
- [release/store-upload-runbook.md](release/store-upload-runbook.md) - aktuelle Build-Identität, Upload-Status, Blocker, Hashes und nächste Console-Klicks
- [release-checklist.md](release-checklist.md) - generische Go/No-Go-Checkliste für jeden Release Candidate
- [../manual-test-checklist.md](../manual-test-checklist.md) - Rollen- und Plattform-Smoke-Matrix für manuelle Release-Validierung
- [beta-distribution.md](beta-distribution.md) - Zugangspfad für externe Tester und 5-Minuten-Feedback-Skript
- [workflow-http-api-activepieces.md](workflow-http-api-activepieces.md) - serverseitige HTTP-Workflow-API und Secret-Header-Vertrag

## Recht und Vertrauen

- [legal/terms.md](legal/terms.md)
- [legal/privacy.md](legal/privacy.md)
- [legal/imprint.md](legal/imprint.md)
- [legal/SUBSCRIPTION_TERMS.md](legal/SUBSCRIPTION_TERMS.md)
- [legal/AI_USAGE_NOTICE.md](legal/AI_USAGE_NOTICE.md)
- [compliance/README.md](compliance/README.md)

## Ergänzende Referenzen

- [design/SKYOS_EXPERIENCE_PRINCIPLES.md](design/SKYOS_EXPERIENCE_PRINCIPLES.md)
- [localization-terminology-glossary.md](localization-terminology-glossary.md)
- [localization-audit.md](localization-audit.md)
- [localization-roadmap.md](localization-roadmap.md)
- [ai-membership-funnel-metrics.md](ai-membership-funnel-metrics.md)
- [ai-subscriptions-rollout-brief.md](ai-subscriptions-rollout-brief.md)
- [store/app-store.md](store/app-store.md)
- [store/google-play.md](store/google-play.md)
- [store/screenshots.md](store/screenshots.md)
- [store/review-prep.md](store/review-prep.md)

## Kompatibilitätshinweise

Ältere Dokumente in Großbuchstaben wie `DEVELOPER_GUIDE.md`, `OWNER_GUIDE.md`,
`LEGAL_OVERVIEW.md` und `RELEASE_CHECKLIST.md` bleiben im Repository, damit ältere Verweise weiter
funktionieren. Die kanonischen Versionen leben inzwischen in den kleingeschriebenen Dateien oben.

## Betriebsstandard

Wenn eine Änderung Nutzervertrauen, Produktverhalten, Berechtigungen, Billing, Rechtstexte oder den
Release-Prozess betrifft, sollte das passende Dokument im gleichen Pull Request oder Commit
aktualisiert werden. SkyOS sollte sich nicht auf stilles Teamwissen verlassen.
