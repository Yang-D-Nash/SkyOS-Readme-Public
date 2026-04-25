<p align="center">
  <img src="docs/assets/skyos-logo-original.png" alt="SkyOS Logo" width="360" />
</p>

<h1 align="center">SkyOS</h1>
<p align="center">
  Das zentrale Betriebskern-System fuer AI, Music, Video und Commerce auf <strong>macOS, iOS und Android</strong>.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/platform-macOS%20%7C%20iOS%20%7C%20Android-0A84FF?style=for-the-badge" alt="Platforms" />
  <img src="https://img.shields.io/badge/architecture-KMP%20%2B%20SwiftUI%20%2B%20Compose-111827?style=for-the-badge" alt="Architecture" />
  <img src="https://img.shields.io/badge/status-v1.0.0-16A34A?style=for-the-badge" alt="Status" />
</p>

---

## Hero

SkyOS ist das Fundament dieses Projekts: Design-Sprache, Betriebslogik, Motion, Brand-System und Runtime laufen aus einem Kern.
Alle Produktflaechen sind Teil eines Systems, nicht lose Einzel-Apps:

- `Home` als Einstieg in die SkyOS-Welt
- `AI` und `Agent` als produktiver Workspace
- `Music` als kuratierte `ZweiZwei / 22`-Zone
- `Video` als fokussierte Content-Surface
- `Shop` als `Skydown x 22`-Commerce-Pfad

## Visual Identity

<p align="center">
  <img src="docs/assets/skyos-logo-original.png" alt="SkyOS Original Logo" width="220" />
  <img src="docs/assets/skydown-logo-original.png" alt="Skydown Original Logo" width="220" />
  <img src="docs/assets/zweizwei-logo-original.png" alt="ZweiZwei Original Logo" width="220" />
</p>

| Marke | Rolle | Primärer Einsatz |
| --- | --- | --- |
| `SkyOS` | Systemkern, Plattformidentität, App-Icon | Home/System, Launcher, Architektur |
| `Skydown` | Produktidentität | AI, Video, Core-Produktkommunikation |
| `ZweiZwei / 22` | Music-Identität | Music-Screens, Artist-/Release-Kontext |
| `Skydown x 22` | Merch-Kollaboration | Shop, Produkt- und Commerce-Surfaces |

## At A Glance

| Bereich | Premium-Kriterium | Status |
| --- | --- | --- |
| Branding | SkyOS-first mit klarer Sub-Brand-Trennung | `PASS` |
| Icon-System | iOS, iPadOS, macOS, Android vollständig | `PASS` |
| Store-Readiness | Listing + Screenshot-Playbook vorhanden | `PASS` |
| Plattform-Konsistenz | SwiftUI + Compose + KMP kohärent | `PASS` |

## App Icon Gallery

Aktives Master-Icon (derzeit im Projekt ausgerollt):

<p align="center">
  <img src="docs/assets/skyos-app-icon.png" alt="SkyOS Active App Icon Master" width="220" />
</p>

Premium-Varianten (A/B/C) für Brand- und Store-Testing:

<p align="center">
  <img src="docs/assets/icon-variants/A-original-premium/master-1024.png" alt="SkyOS Icon Variant A Original Premium" width="180" />
  <img src="docs/assets/icon-variants/B-minimal-legacy/master-1024.png" alt="SkyOS Icon Variant B Minimal Legacy" width="180" />
  <img src="docs/assets/icon-variants/C-luxury-dark/master-1024.png" alt="SkyOS Icon Variant C Luxury Dark" width="180" />
</p>

| Variante | Stil | Einsatzempfehlung |
| --- | --- | --- |
| `A` Original Premium | maximal markennah, aus Originalwelt abgeleitet | Brand-Konsistenz / Default-Baseline |
| `B` Minimal Legacy | ruhiger, reduzierter Look | kleine Größen, klare Launcher-Lesbarkeit |
| `C` Luxury Dark | kontraststark, cineastisch | Premium-Kampagnen / Store-Visual-Tests |

> Empfehlung: `A` als stabile Release-Baseline, `B/C` für kontrollierte Store-Experimente.

## Vision

SkyOS steht fuer eine Premium-Produktplattform mit ruhiger, klarer und vertrauenswuerdiger UX.  
Ziel ist eine konsistente Erfahrung ueber Desktop und Mobile, bei der alle Features technisch und visuell aus einem System kommen.

## Projektstatus

| Bereich | Stand |
| --- | --- |
| Produktstatus | `SkyOS v1.0.0 build-faehig mit Legal-Review-Vorbehalt` |
| Branch | `main` |
| Plattform-Fokus | `macOS`, `iOS`, `Android` |
| Letzter dokumentierter Smoke | Android `PASS`, iOS `PASS` |
| Design-Prinzip | SkyOS-first, systemisch statt modular |

## Quick Navigation

| Fokus | Dokument |
| --- | --- |
| Architektur | [docs/architecture.md](docs/architecture.md) |
| Branding | [docs/branding.md](docs/branding.md) |
| iOS | [docs/ios.md](docs/ios.md) |
| Android | [docs/android.md](docs/android.md) |
| Store Listing | [docs/store-listing.md](docs/store-listing.md) |
| Screenshot-System | [docs/store-screenshots.md](docs/store-screenshots.md) |
| Release | [docs/release-checklist.md](docs/release-checklist.md) |

## Feature Cards

| Bereich | Rolle im System | Ergebnis |
| --- | --- | --- |
| SkyOS Core | Betriebskern, Design, Motion, Branding | Einheitliche Produktidentitaet |
| AI / Agent | Produktiver Workspace fuer Assistenz und Ausfuehrung | Klarer Kontext, Antworten, Status |
| Music (`ZweiZwei / 22`) | Eigenstaendige Musikidentitaet innerhalb von SkyOS | Artist- und Release-Fokus |
| Video | Fokusflaeche fuer Clips und Visuals | Ruhiger Medienfluss ohne Feed-Chaos |
| Shop (`Skydown x 22`) | Commerce-Pipeline von Produkt bis Checkout | Vertrauenswuerdiger Kaufpfad |
| Legal / Trust | Transparenz, Governance, Richtlinien | Release-faehige, saubere Kommunikation |

## Architekturuebersicht

```mermaid
flowchart TB
    skyos["SkyOS Core (System + Motion + Brand)"]
    skyos --> ios["Apple Apps (iOS / iPadOS / macOS)"]
    skyos --> android["Android App"]
    skyos --> shared["Shared KMP Domain"]
    ios --> firebase["Firebase Backend"]
    android --> firebase
    shared --> ios
    shared --> android
    firebase --> functions["Cloud Functions"]
    functions --> ai["AI Runtime"]
    functions --> commerce["Commerce / Checkout"]
    functions --> trust["Auth / Legal / Governance"]
```

## Tech Stack

| Layer | Technologie |
| --- | --- |
| Apple Client | SwiftUI, Xcode, Asset Catalog (`.xcassets`) |
| Android Client | Kotlin, Jetpack Compose, Android Gradle |
| Shared Domain | Kotlin Multiplatform (`shared/`) |
| Backend | Firebase Auth, Firestore, Storage, Cloud Functions |
| Dokumentation | Markdown, Store/Release/Legal-Dokumente |

## Plattformen und Build

| Plattform | Modul | Build-Referenz |
| --- | --- | --- |
| macOS / iOS | `Skydown App.xcodeproj` | `xcodebuild` mit passender Destination |
| Android | `androidApp/` | `./gradlew :androidApp:assembleRelease` |
| Shared | `shared/` | Wird in Apple- und Android-Builds eingebunden |
| Functions | `functions/` | `npm ci --prefix functions`, `npm run build --prefix functions`, `npm test --prefix functions` |

```bash
# Backend
npm ci --prefix functions
npm run build --prefix functions
npm test --prefix functions

# Android
./gradlew :androidApp:assembleRelease

# Apple (iOS Simulator Beispiel)
xcodebuild -project "Skydown App.xcodeproj" -scheme "Skydown App" -configuration Debug -destination "generic/platform=iOS Simulator" build
```

Fuer lokale Release-Smokes ohne Store-Signing kann Android mit
`./gradlew :androidApp:assembleRelease -PallowDebugReleaseSigning=true` gebaut werden. Store-faehige
Builds benoetigen `keystore.properties` oder `SKYOS_UPLOAD_*` Secrets.

## App-Icons und Branding

Diese vier Dateien sind die **Single Source of Truth** fuer die Markenlogos:

| SoT Logo | Zweck | Pfad |
| --- | --- | --- |
| SkyOS | System-/Launcher-Identitaet | `docs/assets/skyos-logo-original.png` |
| Skydown | Produkt-/Operator-Identitaet | `docs/assets/skydown-logo-original.png` |
| ZweiZwei | Music-Identitaet | `docs/assets/zweizwei-logo-original.png` |
| Skydown x 22 | Merch-Kollaborations-Identitaet | `docs/assets/skyos-22-mark.png` |

Ableitung im Projekt (aus den SoT-Logos):

- iOS In-App-Branding: `Skydown App/Assets.xcassets/*Brand*.imageset`
- iOS/macOS App-Icon-Slots: `Skydown App/Assets.xcassets/AppIcon.appiconset/*`
- Android In-App-Branding: `androidApp/src/main/res/drawable-nodpi/*_brand_logo.png`
- Android Launcher-Icons: `androidApp/src/main/res/mipmap-*/ic_launcher.png`
- Android Adaptive Foreground: `androidApp/src/main/res/drawable-nodpi/ic_launcher_foreground_src.png`

## README Navigation

- [Architektur](docs/architecture.md)
- [Branding](docs/branding.md)
- [iOS](docs/ios.md)
- [Android](docs/android.md)
- [Backend](docs/backend.md)
- [Deployment](docs/deployment.md)
- [Store-Dokumente](docs/store/README.md)
- [Store Listing](docs/store-listing.md)
- [Store Screenshots](docs/store-screenshots.md)
- [Release-Checkliste](docs/release-checklist.md)

## Roadmap

| Phase | Fokus | Zielbild |
| --- | --- | --- |
| R1 - Stabilisierung | Build, Smoke, Legal-Konsistenz | Vollstaendig release-faehiger Hauptzweig |
| R2 - Plattform-Finish | macOS/iOS/Android QA, Asset-Schliff | Ein konsistentes SkyOS-Produkt auf allen Plattformen |
| R3 - Produkttiefe | AI-Workflows, Commerce, Membership | Hoehere Conversion und klarere Nutzerfuehrung |
| R4 - Operativer Ausbau | Monitoring, Support, Governance | Belastbarer Produktionsbetrieb |

## Release und Compliance

Der technische Stand ist release-nah. Vor oeffentlicher Auslieferung bleibt die juristische Finalfreigabe fuer alle Rechtstexte verpflichtend.

Relevante Dokumente:

- `docs/legal/privacy.md`
- `docs/legal/terms.md`
- `docs/legal/imprint.md`
- `docs/legal/AI_USAGE_NOTICE.md`
- `docs/legal/SUBSCRIPTION_TERMS.md`

## Lizenz

Aktuell projektspezifisch. Falls benoetigt, Lizenzdatei zentral in `LICENSE` ergaenzen.
