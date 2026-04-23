<p align="center">
  <img src="docs/assets/skyos-logo.png" alt="SkyOS logo" width="196" />
</p>

<p align="center">
  <img src="docs/assets/skyos-app-icon.png" alt="SkyOS app icon" width="104" />
</p>

<h1 align="center">SkyOS</h1>

<p align="center">
  <strong>AI • Creator • Commerce Plattform</strong>
</p>

<p align="center">
  Natives iOS- und Android-Produktsystem fuer Assistant-grade AI, Creator-Media, Merch-Commerce,
  Membership und Owner-Governance in einer kontrollierten Plattform.
</p>

<p align="center">
  <a href="docs/README.md">Dokumentation</a> •
  <a href="docs/architecture.md">Architektur</a> •
  <a href="docs/deployment.md">Deployment</a> •
  <a href="docs/legal/terms.md">AGB</a> •
  <a href="docs/legal/privacy.md">Datenschutz</a> •
  <a href="https://github.com/Yang-D-Nash/SkyOs-App">Repository</a>
</p>

<p align="center">
  <img alt="iOS SwiftUI" src="https://img.shields.io/badge/iOS-SwiftUI-0A84FF?style=for-the-badge&logo=apple&logoColor=white" />
  <img alt="Android Compose" src="https://img.shields.io/badge/Android-Compose-34A853?style=for-the-badge&logo=android&logoColor=white" />
  <img alt="Firebase Backend" src="https://img.shields.io/badge/Backend-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" />
  <img alt="Shared Kotlin Multiplatform" src="https://img.shields.io/badge/Shared-Kotlin%20Multiplatform-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
</p>

<p align="center">
  <img alt="Status" src="https://img.shields.io/badge/Status-Interne%20RC%20Baseline-101828?style=flat-square" />
  <img alt="Trust" src="https://img.shields.io/badge/Trust-Security%20und%20Legal%20gated-475467?style=flat-square" />
</p>

> Security-first. Firebase-basiert. Natives iOS + Android. Kontrollierte AI-Laufzeit. Datenschutzbewusst aufgebaut. Owner-Governance mit klaren Eingriffspunkten.

SkyOS ist die Premium-Produktschicht des Skydown-Oekosystems. Die Plattform verbindet eine taegliche
Home-Oberflaeche, Assistant- und Agent-grade AI, Creator-Media, Merch-Commerce, Membership-Logik
und operative Steuerung in einem nativen System statt in einem Stapel voneinander getrennter Tools.

Das Ziel ist nicht Feature-Masse. Das Ziel ist ein Produkt, das klar wirkt, kommerziell glaubwuerdig
ist, operativ gefuehrt werden kann und sich auf echten Geraeten release-faehig anfuehlt.

## Was ist SkyOS

SkyOS kombiniert:

- eine native AI-Schicht fuer Assistant, FAQ, Visual-Generierung und agentische Ausfuehrung
- Creator-Media-Flaechen fuer Musik, Video, Beats und Artist-Praesenz
- Commerce-Infrastruktur fuer Merch, Checkout-Start, Orders und Support-Sichtbarkeit
- membership-aware Zugriff, Restore-Pfade und Upgrade-Logik
- Owner-/Admin-Steuerung fuer Runtime-Settings, Legal-Content, Revenue-Operations und Release-Hygiene
- eine Firebase-gestuetzte Trust-Layer mit Rules, App Check, Cloud Functions und operativen Schutzmechanismen

## Warum SkyOS

SkyOS loest ein Produktproblem, nicht nur ein technisches:

- ein mobiles Produkt statt getrennter AI-, Creator-, Shop- und Admin-Inseln
- wiederkehrender Nutzwert durch Assistant-Utility, Media, Membership und Commerce in einem System
- Founder-grade Kontrolle ueber Runtime-Verhalten, Berechtigungen, Legal-Content und Release-Readiness
- native Qualitaet auf iPhone und Android statt eines Lowest-common-denominator Wrappers
- ein Repository, das zugleich Produktoberflaeche und Betriebssystem fuer das Team dahinter ist

## Kernmodule

| Modul | Rolle | Business-Wert |
| --- | --- | --- |
| Home | Startflaeche, Einstiegspunkte, Signale und Produkthierarchie | Macht die Plattform ab der ersten Session verstaendlich |
| AI | Bot, FAQ, Visual-Generierung und Agent-Workflows | Schafft wiederkehrenden Nutzen ueber passiven Content hinaus |
| Music und Video | Tracks, Media, Artist-Identitaet und Creator-Praesentation | Verankert das Produkt in Content und Relevanz |
| Merch und Orders | Storefront, Cart, Checkout-Handoff und Order-Sichtbarkeit | Macht aus Aufmerksamkeit Umsatz, ohne das Oekosystem zu verlassen |
| Membership | Zugriffskontrolle, Restore, Upgrade-Logik und plan-aware Limits | Stuetzt Monetarisierung mit klarem Wert statt Clutter |
| Owner Control | Runtime-Settings, Legal-Content, Revenue-Ops und Rollensteuerung | Gibt dem Team Hebel ohne separates Backoffice |
| Trust Layer | Security Rules, App Check, Support-Pfade und Kill Switches | Macht das Produkt steuerbar statt nur demo-faehig |

## Plattformen

- Native iOS-App in [`Skydown App/`](<Skydown App/>)
- Native Android-App in [`androidApp/`](androidApp/)
- Shared Kotlin Multiplatform Model-Layer in [`shared/`](shared/)
- Firebase-Backend mit Auth, Firestore, Storage, Cloud Functions, Rules und App Check
- Optionale externe Automationspfade fuer nutzereigene Activepieces- oder `n8n`-Setups sowie optionales Manus BYOS fuer Agent-Workloads

## Architektur-Snapshot

SkyOS besteht aus zwei nativen Mobile-Clients auf einer kontrollierten Firebase-Basis mit
gemeinsamer Model-Layer, server-autoritativem Handling privilegierter Mutationen und
owner-gesteuerten Runtime-Controls.

```mermaid
flowchart LR
    iOS["iOS App (SwiftUI)"] --> Firebase["Firebase Core"]
    Android["Android App (Compose)"] --> Firebase
    Shared["Shared Models (KMP)"] --> iOS
    Shared --> Android
    Firebase --> Auth["Auth"]
    Firebase --> Firestore["Firestore"]
    Firebase --> Storage["Storage"]
    Firebase --> Functions["Cloud Functions"]
    Functions --> AI["AI Runtime und Provider Controls"]
    Functions --> Commerce["Commerce und Orders"]
    Functions --> Ops["Owner- und Governance-Control"]
    Commerce --> Stripe["Stripe oder aktivierte Payment Rails"]
    Commerce --> Shopify["Shopify"]
    Ops --> External["Activepieces / n8n / Manus (optional)"]
    Functions --> Stores["App Store und Play Billing Sync"]
```

Zentrale technische Referenzen:

- [docs/architecture.md](docs/architecture.md)
- [docs/backend.md](docs/backend.md)
- [docs/ios.md](docs/ios.md)
- [docs/android.md](docs/android.md)

## AI / Agent-System

SkyOS AI ist als kontrollierte Produktschicht gedacht, nicht als isolierter Chatbot.

- Assistant, FAQ, Visual-Generierung und Agent-Flows leben direkt in der Hauptoberflaeche
- Provider-Routing, Runtime-Settings und Usage-Policy bleiben owner-kontrolliert
- AI-Verfuegbarkeit wird ueber Account-Zustand, Entitlement-Logik und Backend-Autoritaet gesteuert
- operative Sicherheit umfasst Rate-Grenzen, Pause-States, History-Retention-Regeln und Review-Pfade

Mehr dazu in [docs/ai-system.md](docs/ai-system.md).

## Commerce / Membership

Commerce ist Teil des Produkts und kein angeflanschtes Widget.

- Merch-Discovery, Cart-Verhalten, Checkout-Vorbereitung und Order-Sichtbarkeit liegen im selben Nutzerfluss
- Membership-Logik steuert entitlement-aware Zugriff, Restore, Upgrade-Hinweise und wiederkehrende Value-Surfaces
- Hosted Checkout, Payment-Konfiguration und Live-Billing-Readiness werden backend-gesteuert und release-seitig gegatet

Mehr dazu in [docs/commerce.md](docs/commerce.md).

## Owner Control

SkyOS enthaelt Governance-Flaechen fuer das operative Team hinter dem Produkt.

- Runtime-Controls fuer AI, Commerce und Account-Policies
- Legal-Content, rollenabhaengige Settings und Release-Hygiene
- Revenue-Operations, support-sensible Flows und Kill Switches fuer Incident Response
- kontrollierte Integrationspunkte fuer externe Workflows und Automation

Mehr dazu in [docs/owner-admin.md](docs/owner-admin.md).

## Sicherheitsprinzipien

- UI-Sichtbarkeit ist nie die letzte Berechtigungsgrenze
- privilegierte Mutationen gehoeren in Cloud Functions und Rules, nicht nur in Client-Code
- Firestore und Storage arbeiten standardmaessig mit expliziten, rollenbewussten Zugriffspruefungen
- produktive Secrets gehoeren in sichere Runtime-Stores, nicht in Git
- App Check, Release-Smokes und Runtime-Locks sind Teil des Hardening, nicht optionales Polish
- privacy-sensitive Flows sollen nur Daten erfassen, die fuer den jeweiligen Produktpfad wirklich noetig sind
- Owner-Governance-Controls dienen dazu, Vorfaelle kontrolliert zu begrenzen, statt erst blind Hotfixes zu shippen

## Dokumentationsindex

### Kern-Dokumente

- [docs/README.md](docs/README.md)
- [docs/architecture.md](docs/architecture.md)
- [docs/backend.md](docs/backend.md)
- [docs/ios.md](docs/ios.md)
- [docs/android.md](docs/android.md)
- [docs/ai-system.md](docs/ai-system.md)
- [docs/commerce.md](docs/commerce.md)
- [docs/owner-admin.md](docs/owner-admin.md)
- [docs/deployment.md](docs/deployment.md)
- [docs/release-checklist.md](docs/release-checklist.md)
- [docs/branding.md](docs/branding.md)
- [docs/faq.md](docs/faq.md)
- [docs/store/README.md](docs/store/README.md)
- [docs/store/app-store.md](docs/store/app-store.md)
- [docs/store/google-play.md](docs/store/google-play.md)
- [docs/store/screenshots.md](docs/store/screenshots.md)
- [docs/store/review-prep.md](docs/store/review-prep.md)

### Legal-Dokumente

- [docs/legal/terms.md](docs/legal/terms.md)
- [docs/legal/privacy.md](docs/legal/privacy.md)
- [docs/legal/imprint.md](docs/legal/imprint.md)

## Schnellstart

### 1. Klonen

```bash
git clone https://github.com/Yang-D-Nash/SkyOs-App.git
cd SkyOs-App
```

### 2. Installieren

```bash
npm ci --prefix functions
```

### 3. iOS starten

Oeffne `Skydown App.xcodeproj` in Xcode fuer Simulator- oder Device-Arbeit oder baue ueber die Kommandozeile:

```bash
xcodebuild -project "Skydown App.xcodeproj" -scheme "Skydown App" -configuration Debug -destination "generic/platform=iOS Simulator" build
```

### 4. Android starten

```bash
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:assembleDebugAndroidTest
```

### 5. Functions starten

```bash
npm test --prefix functions
```

Live-Service-Pfade benoetigen die erwarteten Firebase-Konfigurationsdateien und sichere Runtime-Secrets.
Siehe [docs/backend.md](docs/backend.md) und [docs/deployment.md](docs/deployment.md).

## Deployment

Deployment wird als kontrollierte Release-Aktion verstanden, nicht als blinder Sync.

- nutze [docs/deployment.md](docs/deployment.md) fuer Deploy- und Rollback-Ablauf
- nutze [docs/release-checklist.md](docs/release-checklist.md) fuer das finale Launch-Gating
- validiere Billing, Legal-Content, Rules und Real-Device-Smokes vor einem oeffentlichen Release

Wichtige Kommandos:

```bash
firebase deploy --only functions
firebase deploy --only firestore:rules,storage
firebase deploy --only functions:syncShopifyMerch,functions:startAiSubscriptionCheckout
```

## Rechtliches

SkyOS fuehrt seine Arbeitsgrundlage fuer Legal direkt im Repository, damit Produkt, Ops und Release
koordiniert bleiben.

- [AGB](docs/legal/terms.md)
- [Datenschutz](docs/legal/privacy.md)
- [Impressum](docs/legal/imprint.md)
- [Subscription Terms](docs/legal/SUBSCRIPTION_TERMS.md)
- [AI Usage Notice](docs/legal/AI_USAGE_NOTICE.md)

Diese Dokumente bilden eine professionelle Foundation, ersetzen vor einem oeffentlichen Launch aber
keine finale Betreiber- und marktspezifische Rechtspruefung.

## Status

Aktuelle Repository-Baseline:

- native iOS- und Android-Produktfundamente sind vorhanden
- Firebase-Backend, Owner-Controls, Merch, Membership und AI-Fundamente stehen
- Release-, Legal-, Branding- und Deployment-Dokumentation liegen im Repo
- das Repository ist so strukturiert, dass Produktreview, Developer-Onboarding und operative Disziplin moeglich sind

Vor einem oeffentlichen Release weiter noetig:

- finale rechtliche Freigabe fuer Betreiber und Zielmaerkte
- Live-Billing- und Store-Validierung auf Release-Kandidaten
- finaler Localization- und Copy-Consistency-Pass
- finale Real-Device-Regression ueber kritische Kernflows
- Monitoring-, Analytics- und Support-Readiness bestaetigen
