# SkyOS

SkyOS ist ein Premium Creator Operating System fuer iOS und Android.  
Es verbindet Home, AI, Music, Video, Shop, Profile und Settings in einem konsistenten System statt in isolierten Modulen.

## Vision

- Creator-first Produkt mit klarer Hierarchie statt Feature-Chaos
- Premium UX mit ruhiger, vertrauenswuerdiger Bedienung
- Skalierbare Membership-, AI- und Operations-Basis

## Plattformen

- iOS (`Skydown App`, SwiftUI)
- Android (`androidApp`, Jetpack Compose)
- Backend (`Firebase`, `Cloud Functions`, Firestore, App Check)

## Kernbereiche

- `Home`: Daily Operating Surface
- `AI`: Bot, Agent, Membership, Guardrails
- `Music` / `Video`: Creator Content Hubs
- `Shop`: Commerce, Orders, Fulfillment
- `Profile`: Identity + personal dashboard
- `Settings`: Control Center, Legal, Runtime, Admin/Ops

## Quick Start

1. iOS build:
   - `xcodebuild -project "Skydown App.xcodeproj" -scheme "Skydown App" -configuration Debug -destination "generic/platform=iOS Simulator" build`
2. Android build:
   - `./gradlew :androidApp:assembleDebug`
3. Backend functions (optional):
   - `cd functions && npm install`

## Dokumentation

- `docs/USER_GUIDE.md`
- `docs/OWNER_GUIDE.md`
- `docs/ADMIN_GUIDE.md`
- `docs/CREATOR_GUIDE.md`
- `docs/DEVELOPER_GUIDE.md`
- `docs/ARCHITECTURE.md`
- `docs/LOCALIZATION_GUIDE.md`
- `docs/LEGAL_OVERVIEW.md`
- `docs/RELEASE_CHECKLIST.md`

## Legal Hinweis

Die im Repository enthaltenen Rechtstexte sind fuer Produktbetrieb und In-App-Darstellung vorbereitet, muessen jedoch vor einem externen Launch rechtlich durch qualifizierte Beratung in den Zielregionen final freigegeben werden.
