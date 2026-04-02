# Sky²² / Skydown x 22

| Sky²² | Skydown x 22 | Skydown |
| --- | --- | --- |
| ![22 Logo](<Skydown App/Assets.xcassets/ZweizweiBrandLogo.imageset/zweizwei-logo.png>) | ![Skydown x 22](<Skydown App/Assets.xcassets/SkydownX22BrandLogo.imageset/skydown-x22-logo.png>) | ![Skydown](<Skydown App/Assets.xcassets/SkydownBrandLogo.imageset/skydown-logo.png>) |

Sky²² ist die gemeinsame App fuer `ZweiZwei`, `Skydown` und `Skydown x 22`.

Die App vereint:

- `Music`
- `Video`
- `Merch`
- `AI Tools`
- `Profile`

Der Stand in diesem Repository ist auf einen harten, testfaehigen `Stand 1` fuer mindestens `100 Tester` ausgelegt.

---

## 1. Schnellstart

### Was die App fuer Nutzer ist

Die App ist kein klassischer Shop und kein reiner Musikplayer.

Sie ist ein gemeinsamer Brand-Hub fuer:

- Releases und Artists
- Video- und Reel-Content
- In-App-Merch mit eigenem Checkout
- KI-Tools mit persoenlicher History
- persoenliche Profile mit Galerie

### Die Hauptbereiche

#### Home

- zeigt den aktuell wichtigsten Release
- zeigt Beat- und Video-Highlights
- fuehrt direkt in Music, Video, Shop und Tools

#### Music

- Releases
- Artists
- Beat Hub
- Studio / Nicma

#### Videos

- Video Hub
- YouTube-Inhalte in der App
- Skydown- und Collab-Content

#### Merch

- Produkte kommen aus Shopify
- Checkout bleibt in der App
- Versandkosten werden in der App berechnet

#### Tools

- chat-first KI-Bereich
- persoenliche Bot-/Agent-History pro User
- Limits pro Account

#### Profil

- Username
- Bio
- Profilbild
- Galerie fuer Bilder, Videos und Sounds

---

## 2. Rollenmodell

### Owner

Fester Owner dieser App ist:

- `nash.lioncorna@gmail.com`

Der Owner hat alleinige Kontrolle ueber:

- Shopify
- Zahlarten
- Versand/Commerce
- Rollen und Nutzerverwaltung
- n8n / Automation
- Runtime-Lockdown und Recovery
- sensible Firebase- und Systembereiche

Kurz gesagt:

- `Owner darf alles`

### Admin

Admins sind teaminterne Leute.

Admins bekommen **keine** pauschalen Root-Rechte.
Der Owner weist ihnen gezielt Funktionen zu.

Aktuell zuweisbare Funktionsbereiche:

- `Music verwalten`
- `Video verwalten`
- `Profile moderieren`

Admins haben **keinen** Zugriff auf:

- Shopify-Root-Konfiguration
- Zahlarten
- Owner-Rollenlogik
- n8n-Root-Konfiguration
- Recovery-/Lockdown-Steuerung

### Subadmin

Subadmins sind keine internen Root-Admins mehr.

Sie stehen fuer externe Premium-/Power-Accounts mit groesserem Kontingent.

Subadmins bekommen:

- persoenliche Nutzung
- groessere KI-Kontingente
- laengere History

Subadmins bekommen **nicht**:

- Owner-Rechte
- System-Settings
- operative Admin-Bereiche

Aktuelle Kontingentmodelle fuer Subadmins:

- `Creator`
- `Studio`

### User

Normale User haben:

- persoenliches Konto
- Free-Kontingent
- Profil und Galerie
- persoenliche KI-History

### Gast

Nicht eingeloggte Leute sind zusaetzlich ein eigener Gast-Zustand.

Sie haben:

- keinen persistenten Account
- keine persoenliche Cloud-History
- keine Profilbearbeitung

---

## 3. Kontingente

### Aktuelle Modelle

| Modell | Ziel | Bot/Tag | Visuals/Tag | Agent/Tag | History |
| --- | --- | ---: | ---: | ---: | ---: |
| `Owner Unlimited` | Owner | 5000 | 1200 | 3000 | 30 Tage |
| `Internal Team` | interne Admins | 240 | 40 | 140 | 30 Tage |
| `Free` | normale User | 30 | 4 | 18 | 3 Tage |
| `Creator` | Premium / Subadmin | 120 | 20 | 70 | 7 Tage |
| `Studio` | groesseres Premium-Modell | 240 | 40 | 140 | 30 Tage |

### Wichtige Logik

- Owner bleibt praktisch unlimitiert fuer Betrieb und Tests.
- Admins bekommen ihre Funktion ueber Toggles, nicht ueber pauschale Vollmacht.
- User haben standardmaessig `Free`.
- Subadmins koennen auf `Creator` oder `Studio` gesetzt werden.
- Die Limits liegen serverseitig und sind nicht nur UI.

---

## 4. Fuer Nash / Owner

### Was du direkt steuerst

In der App bist du als Owner die zentrale Root-Instanz.

Du steuerst:

- `Settings > Owner`
- `Settings > Users`
- `Settings > Shopify`
- `Settings > Payments`
- `Settings > Commerce`
- `Settings > Automation`

### Was nur du tun sollst

- Shopify Domain / Token / Collection pflegen
- Zahlarten aktivieren oder aendern
- Store oeffnen oder pausieren
- Rollen vergeben
- Admin-Funktionen zuweisen
- Subadmin-Kontingente vergeben
- Lockdown schalten
- n8n verbinden

### Owner-Release-Check

Vor einem Test- oder Release-Run:

1. Android Debug-Build bauen
2. iOS Build bauen
3. Functions Syntax pruefen
4. Rule-Tests laufen lassen
5. Shopify Sync pruefen
6. Tools / Profile / Uploads testen
7. `system/runtimeConfig` pruefen

---

## 5. Fuer Admins

Admins sind fuer operative Bereiche da, nicht fuer System-Root.

### Typische Aufgaben

- Music/Beat-Inhalte pflegen
- Video-Inhalte pflegen
- Profile moderieren
- Backoffice innerhalb zugewiesener Bereiche

### Typische Nicht-Aufgaben

- keine Owner-Rollen vergeben
- keine Shopify-Root-Daten aendern
- keine Zahlungsarten aendern
- keine Lockdown-Steuerung
- kein n8n-Root-Setup

---

## 6. Fuer Subadmins und User

### Subadmins

Subadmins sind externe Premium-Accounts.

Sie koennen je nach zugewiesenem Modell:

- mehr KI nutzen
- laengere History bekommen
- persoenlicher arbeiten

### User

User starten auf:

- `Free`

Sie koennen:

- Profil pflegen
- Galerie aufbauen
- Musik/Video/Merch nutzen
- Tools mit Free-Limits nutzen

---

## 7. Technischer Aufbau

### Frontends

- `iOS`: SwiftUI
- `Android`: Jetpack Compose
- `shared`: Kotlin Multiplatform fuer gemeinsame Modelle/Use Cases

### Backend

- Firebase Auth
- Cloud Firestore
- Cloud Storage
- Cloud Functions

### Wichtige Integrationen

- Shopify fuer Produkt-/Variantenquelle
- PODpartner fuer Produktion / Fulfillment
- Spotify
- YouTube
- Instagram
- n8n

### Projektstruktur

- `Skydown App/` = iOS App
- `androidApp/` = Android App
- `shared/` = gemeinsame Modelle / Services / Use Cases
- `functions/` = Firebase Functions
- `firestore.rules` = Firestore Security Rules
- `storage.rules` = Storage Security Rules

---

## 8. Firebase Security und Cost Control

### Ziel

Die Testumgebung ist absichtlich defensiv gebaut, damit bis zu `100 Tester` moeglich sind, ohne lockere Demo-Loesungen und ohne offenes Kostenrisiko.

### Bereits gehaertet

- `deny by default` fuer Firestore
- `deny by default` fuer Storage
- App Check vorbereitet fuer iOS und Android
- Upload-Slot-Flow fuer Profil/Galerie
- Upload-Limits
- Runtime-Lockdown
- serverseitige KI-Limits
- Owner-only Root-Bereiche

### Wichtige Standardwerte

- `App Check Mode`: `monitor`
- `lockdown`: `false`
- `uploadsEnabled`: `true`
- `registrationsEnabled`: `true`
- `userWritesEnabled`: `true`
- `maxGalleryImagesPerUser`: `10`
- `maxUploadsPer24Hours`: `20`
- `maxImageBytes`: `5 MB`
- erlaubte Bildtypen:
  - `image/jpeg`
  - `image/png`
  - `image/webp`

### Runtime Config

Dokument:

- `system/runtimeConfig`

Empfohlener Start:

```json
{
  "lockdown": false,
  "uploadsEnabled": true,
  "registrationsEnabled": true,
  "userWritesEnabled": true,
  "appCheckMode": "monitor",
  "budgetLockdownEnabled": false,
  "lastLockdownReason": ""
}
```

### Lockdown

Lockdown blockiert fuer normale User:

- neue Uploads
- neue Registrierungen
- normale Schreibzugriffe

Owner behaelt Recovery-Zugriff.

---

## 9. App Check Rollout

### Android

- Provider: `Play Integrity`
- Integration im fruehen App-Start
- Debug trennt sich sauber von Release

### iOS

- Provider: `App Attest`
- Fallback: `DeviceCheck`
- Debug/Simulator sauber getrennt

### Rollout-Reihenfolge

1. in Firebase Console auf `monitor` bleiben
2. Debug Tokens fuer lokale Tests hinterlegen
3. Android auf echtem Geraet testen
4. iOS auf echtem Geraet testen
5. Firestore/Storage/Functions pruefen
6. erst danach `enforce`

Mehr Details:

- [app-check-rollout.md](app-check-rollout.md)
- [manual-test-checklist.md](manual-test-checklist.md)

---

## 10. Shopify / Merch

### Prinzip

- Produkte kommen aus Shopify
- PODpartner produziert und versendet
- Checkout bleibt in der App
- Shopify Checkout wird nicht benutzt

### Owner-Aufgaben

- Store Domain pflegen
- Storefront Access Token pflegen
- optional Collection Handle pflegen
- Shopify Sync starten

### Wichtig

Nur der Owner pflegt:

- Shopify Root-Daten
- Store-Steuerung
- Commerce-/Payment-Konfiguration

---

## 11. Automation / n8n

Die App bleibt normal ueber Firebase eingeloggt.

n8n ist als serverseitige Workflow-Bruecke gedacht.

### Owner-Aufgaben

- Base URL pflegen
- Webhook Path pflegen
- Header setzen
- Test-Webhook senden

Admins und User bekommen keinen Root-Zugriff auf diese Verbindung.

---

## 12. Setup

### iOS

```bash
xcodebuild -project 'Skydown App.xcodeproj' -scheme 'Skydown App' -destination 'generic/platform=iOS' build
```

### Android

```bash
./gradlew :androidApp:compileDebugKotlin
./gradlew :androidApp:assembleDebug
```

### Functions / Rules

```bash
node --check functions/index.js
cd functions
npm run test:rules
```

### Firebase Deploy

```bash
firebase deploy --only firestore:rules
firebase deploy --only storage
firebase deploy --only functions
```

---

## 13. Testplan fuer 100 Tester

### Mindeststand vor Verteilung

- Android Build gruen
- iOS Build gruen
- Rule-Tests gruen
- keine offenen Crashs im Standard-Flow
- App Check im Monitor-Mode
- Upload-Limits aktiv
- Lockdown-Dokument vorhanden
- Owner-Root-Zugriff getestet
- Shopify Sync getestet

### Kritische Testfaelle

- Login / Registrierung
- Profil speichern
- Profilbild hochladen
- Galerie hochladen
- KI Limits / History
- Merch laden
- Cart / Order
- Beat Hub
- Video Hub
- Lockdown aktiviert / deaktiviert

---

## 14. Was bewusst unveraendert bleibt

Diese Codebasis wurde **nicht** aus Spass neu gebaut.

Bewusst respektiert wurden:

- bestehender Firebase-Stack
- bestehendes Rollenmodell `owner / admin / subadmin / user`
- bestehende App-Architektur
- bestehender Checkout
- bestehende Profile-/Galerie-Logik

Geaendert wurde nur dort, wo es fuer:

- Sicherheit
- Kostenkontrolle
- Rollenhaerte
- Owner-Kontrolle
- Testbarkeit
- UX/Release-Polish

noetig war.
