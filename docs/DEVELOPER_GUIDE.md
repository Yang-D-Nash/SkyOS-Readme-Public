# SkyOS Developer Guide

## Repository Struktur

- `Skydown App/` iOS SwiftUI App
- `androidApp/` Android Compose App
- `functions/` Firebase Cloud Functions
- `shared/` geteilte KMP-Modelle/Logik
- `docs/` Produkt-, Operations- und Legal-Dokumentation

## iOS Setup

1. Xcode aktuell halten.
2. `GoogleService-Info.plist` korrekt einbinden.
3. Build:
   - `xcodebuild -project "Skydown App.xcodeproj" -scheme "Skydown App" -configuration Debug -destination "generic/platform=iOS Simulator" build`

## Android Setup

1. Android Studio + JDK entsprechend Gradle-Setup.
2. `google-services.json` und lokale Build-Config korrekt setzen.
3. Build:
   - `./gradlew :androidApp:assembleDebug`

## Firebase und Functions

- Firestore, Auth, Storage, Functions, App Check und Rules sind produktkritisch.
- Functions:
  - `cd functions`
  - `npm install`
  - `npm run lint` (wenn konfiguriert)

## Deployment Grundsaetze

- Erst Build + Smoke, dann Release.
- Keine Runtime-/Rules-Aenderung ohne klaren Rollback-Plan.
- Security Rules und Legal-Inhalte vor Release querpruefen.

## Secrets und Config

- Keine Secrets im Repo committen.
- Nur vorgesehene Konfigurationspfade nutzen (`.example` Dateien beachten).
