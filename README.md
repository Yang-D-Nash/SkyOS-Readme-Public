# Sky²² / Skydown x 22

Sky²² ist eine plattformübergreifende App für Musik, Video, Merch und KI-gestützte Workflows im Umfeld von `ZweiZwei`, `Skydown` und `Skydown x 22`.

Die App läuft auf:

- `iOS` mit `SwiftUI`
- `Android` mit `Jetpack Compose`
- gemeinsamer Modell- und Business-Logik über `Kotlin Multiplatform`

---

## Firebase Security Hardening

### Architekturueberblick

Der sichere Testbetrieb ist jetzt auf `deny-by-default` ausgelegt:

- `Firestore Rules` blockieren standardmaessig alles
- `Storage Rules` erlauben nur eigene Bild-Uploads in klaren User-Pfaden
- `Custom Claims` steuern Rollen: `owner`, `admin`, `subadmin`, `user`
- `system/runtimeConfig` schaltet Lockdown, Uploads, Registrierungen und Schreibzugriffe
- `requestUploadSlot` vergibt serverseitig Upload-Freigaben vor jedem Profil-/Galerie-Upload
- `uploadSlots` und `uploadUsage` bleiben rein serverseitig
- `Budget Alerts` koennen das Projekt in einen sicheren Lockdown versetzen

### Sichere Standardwerte

Aktueller Default fuer den Testbetrieb:

- `App Check Mode`: `monitor`
- `lockdown`: `false`
- `uploadsEnabled`: `true`
- `registrationsEnabled`: `true`
- `userWritesEnabled`: `true`
- `maxGalleryImagesPerUser`: `10`
- `maxUploadsPer24Hours`: `20`
- `maxImageBytes`: `5 MB`
- erlaubte Dateiarten: `image/jpeg`, `image/png`, `image/webp`

### Setup

#### 1. Runtime Config vorbereiten

Lege das Dokument `system/runtimeConfig` an:

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

#### 2. Owner Claim setzen

Setze fuer `nash.lioncorna@gmail.com` die Rolle `owner`.

Admin-SDK-Beispiel:

```js
const admin = require("firebase-admin");

await admin.auth().setCustomUserClaims("<uid>", {
  role: "admin",
  isAdmin: true,
  isStaff: true,
  isOwner: false,
});
```

Im App-/Functions-Flow gibt es dafuer zusaetzlich die Owner-only Callable `setUserRole`.

#### 3. App Check vorbereiten

Web-Referenzen liegen in:

- `examples/web/firebase-app-check.example.js`
- `examples/web/request-upload-slot.example.js`

Native Clients sind jetzt ebenfalls vorbereitet:

- `iOS`: `FirebaseAppCheckProvider.swift` nutzt im Debug/Simulator den Debug-Provider, auf echten Geraeten `App Attest`, sonst `DeviceCheck`
- `Android`: `SkydownApplication.kt` nutzt im Debug-Build den Debug-Provider und im Release `Play Integrity`

Wichtig fuer lokale Tests:

- iOS/Simulator und Android/Debug geben einen Debug-Token aus
- diesen Token in Firebase Console unter `App Check > Debug tokens` hinterlegen
- erst danach echte `enforce`-Rollouts aktivieren

Rollout:

1. `monitor`
2. Metrics und Logs pruefen
3. erst danach `enforce`

#### 4. Deploy-Reihenfolge

```bash
firebase deploy --only firestore:rules
firebase deploy --only storage
firebase deploy --only functions
```

#### 5. Emulator-Tests

```bash
cd functions
npm install
npm run test:rules
```

### Upload-Flow

Der normale sichere Flow ist jetzt:

1. Client fragt `requestUploadSlot` an
2. Backend prueft Lockdown, Tageslimit, Galerie-Limit und Dateityp
3. Client laedt nur in den vom Backend vergebenen Pfad hoch
4. Storage Rules akzeptieren den Upload nur mit passendem Slot
5. Danach schreibt der Client `galleryMeta/{uid}/items/{imageId}` oder aktualisiert `userProfiles/{uid}`

Direkte freie Uploads ohne Slot gehoeren nicht mehr zum normalen Pfad.

### Registrierungen im Lockdown

Es gibt jetzt zwei Schutzebenen:

1. App-Clients pruefen `registrationsEnabled`, bevor ein Sign-up gestartet wird.
2. `enforceRegistrationLockdown` loescht neu angelegte Auth-Accounts wieder, falls waehrend eines Lockdowns trotzdem eine Registrierung durchgeht.

Das ist die kostenguenstigere Variante ohne Identity-Platform-Blocking-Functions. Wenn du Sign-ups schon *vor* der Auth-Erstellung hart stoppen willst, ist spaeter `beforeUserCreated` die naechste Eskalationsstufe.

### Billing Kill Switch

`applyBudgetLockdown` erwartet Budget-Notifications ueber Pub/Sub und setzt dann:

- `lockdown = true`
- `uploadsEnabled = false`
- `registrationsEnabled = false`
- `userWritesEnabled = false`

Das ist absichtlich nicht destruktiv. Dienste laufen weiter, aber neue kostenrelevante Nutzeraktionen werden geblockt.

Ein optionaler, absichtlich nicht aktiver Beispielpfad fuer Billing-Deaktivierung liegt in:

- `functions/src/security/billing-disable.example.js`

### Monitoring

Diese Signale solltest du beobachten:

- Upload-Anzahl pro Tag
- Download-Anzahl
- Storage-Nutzung in Bytes
- Function-Invocations
- Error-Rate / denied requests
- App Check missing / invalid requests
- Auth-Neuregistrierungen waehrend Lockdown

Praktische CLI-Hinweise:

```bash
gcloud pubsub topics create billing-budget-alerts
gcloud pubsub subscriptions create billing-budget-alerts-sub --topic=billing-budget-alerts
```

Danach:

- Budget in Google Cloud Billing anlegen
- Topic `billing-budget-alerts` als Notification-Ziel setzen
- Monitoring Alerts fuer Storage-Bytes, Storage-Requests und Functions-Errors anlegen

### Recovery Guide

Wenn das Projekt in Lockdown ist:

1. Ursache pruefen
2. Traffic-/Kosten-Spitze identifizieren
3. `system/runtimeConfig` nur als `owner` anpassen
4. zuerst `uploadsEnabled` und `registrationsEnabled` bewusst pruefen
5. erst danach `lockdown` wieder deaktivieren

### To-do vor Produktionsstart

- `App Check` von `monitor` auf `enforce` umstellen
- alte ungesicherte Profil-Media-Pfade nicht weiter verwenden
- `uploadSlots` und `uploadUsage` mit TTL/Lifecycle aufraeumen
- Notfall-Runbook fuer Sign-up-Provider in der Console dokumentieren
- fuer oeffentliche Profile spaeter pruefen, ob tokenisierte Download-URLs durch einen haerteren Proxy ersetzt werden sollen

---

## 1. Fuer Nutzer

### Was die App fuer Nutzer sein soll

Sky²² ist keine reine Shop-App und auch keine reine Musik-App. Der Gedanke dahinter ist ein gemeinsamer Hub fuer:

- `Music / ZweiZwei`
- `Videography / Skydown`
- `Merch / Skydown x 22`
- `AI Tools`

Je nach Bereich soll sich die App eher wie eine moderne Medien- und Brand-App anfuehlen als wie ein klassisches Formularsystem.

### Die Hauptbereiche der App

#### Home

Der Home-Bereich ist die Startflaeche der App. Dort landen Nutzer auf:

- aktuellen Releases
- direkten Wegen zu Music, Video und Shop
- persoenlicheren Einstiegen statt rein technischer Menues

#### Music

Im Music-Bereich koennen Nutzer:

- Artists und Releases sehen
- Spotify-Links und Musikzugriffe nutzen
- Beats hoeren
- Beat Hub aufrufen

#### Videos

Im Video-Bereich koennen Nutzer:

- Video-Reels anschauen
- YouTube-Inhalte in der App oeffnen
- Skydown-/Collab-Inhalte ansehen

#### Merch

Der Merch-Bereich zeigt Produkte, Varianten, Cart und Checkout in der App selbst.

Wichtig:

- Der Nutzer kauft nicht ueber den Shopify-Checkout.
- Die App hat ihren eigenen Cart- und Checkout-Flow.
- Versandkosten werden in der App berechnet und vor dem Kauf angezeigt.

#### KI Tools

Der KI-Bereich ist als chat-first Erlebnis gedacht, eher wie `ChatGPT` oder `Gemini` als wie ein kleines Admin-Fenster.

Je nach Freigabe koennen Nutzer:

- Bot-Chats fuehren
- Agent-Funktionen nutzen
- visuelle KI-Aktionen ausloesen

### Login-Zustaende

Es gibt fuer normale Nutzung zwei oeffentliche Zustaende:

- `Gast`: nicht eingeloggt, kein persoenliches Konto, keine gespeicherte KI-History
- `User`: eingeloggt, persoenliche History und persoenliche Limits

Wenn die App oeffentlich genutzt wird, bleibt das fuer die meisten Nutzer die Standardnutzung.

### Datenschutz und Recht

In der App sind direkt erreichbar:

- `AGB`
- `Datenschutzbestimmungen`
- `Nutzungsbedingungen`

Der aktuelle Stand wurde auf `April 2026 / Europa / Deutschland` ausgerichtet.

---

## 2. Interne Technik

### Architektur in Kurzform

Die App ist in vier technische Ebenen aufgeteilt:

1. `Client`
   iOS und Android UI
2. `Shared`
   gemeinsame Models und Business-Regeln
3. `Firebase`
   Auth, Firestore, Storage, Functions
4. `Externe Systeme`
   Shopify, PODpartner, Spotify, n8n, AI

### Repository-Struktur

- `Skydown App/`
  iOS-App mit SwiftUI Views, Services und ViewModels
- `androidApp/`
  Android-App mit Compose Screens, Data Layer und ViewModels
- `shared/`
  gemeinsame KMP-Models und Services
- `functions/`
  Firebase Functions fuer Backend-Logik
- `firestore.rules`
  Firestore-Berechtigungen

### Kernstack

#### Client

- `SwiftUI`
- `Jetpack Compose`
- `Kotlin Multiplatform`

#### Backend

- `Firebase Auth`
- `Cloud Firestore`
- `Firebase Storage`
- `Firebase Functions`
- `Firebase App Distribution`

#### Media / Integrationen

- `Spotify Web API`
- `AVFoundation`
- `Media3 / ExoPlayer`
- `Shopify`
- `PODpartner`
- `n8n`
- `Vertex AI / Gemini`

### Datenhaltung

#### Firestore

Firestore bleibt die zentrale App-Datenbank fuer:

- User
- Order-Metadaten
- App-Konfigurationen
- Merch-Cache / Sichtbarkeit / Overrides
- oeffentliche Video- und Beat-Daten

#### Shopify + PODpartner

Der Merch-Flow ist bewusst getrennt:

- `Shopify` liefert Produkt- und Variantendaten
- `PODpartner` uebernimmt Produktion, Versand und Tracking
- `Firestore` bleibt Cache, Sichtbarkeits- und Override-Schicht
- `die App` behaelt ihren eigenen Checkout

### Merch-Architektur

#### Grundprinzip

Die App bleibt `Shopify-first`, aber nicht `Shopify-checkout-first`.

Das heisst:

- Produkte und Varianten kommen aus Shopify
- die Variantenzuordnung laeuft ueber `shopifyVariantId`
- der Nutzer checkt in der App aus
- nach bestaetigter Zahlung erstellt das Backend eine Shopify-Order
- Shopify / PODpartner uebernehmen danach Fulfillment

#### Shopify-Konfiguration in der App

Die App arbeitet fuer den Katalog mit dem Minimalpfad:

- `Store Domain`
- `Storefront Access Token`
- optional `Collection Handle`

#### Shopify-Order-Flow

Fuer echte externe Orders nutzt das Backend weiter die Admin-Seite von Shopify:

- `GraphQL Admin API`
- `orderCreate`

### n8n / Automation

n8n ist als serverseitige Automations-Bruecke vorbereitet.

Wichtig:

- User loggen sich nicht in n8n ein
- die App bleibt normal ueber Firebase eingeloggt
- der Owner hinterlegt die zentrale n8n-Verbindung
- Firebase Functions schicken geprueften User-Kontext an den Webhook

Der saubere Datenweg ist:

`App -> Firebase Function -> n8n Webhook`

### Build / lokale Pruefung

#### Android

```bash
./gradlew :androidApp:compileDebugKotlin
./gradlew :androidApp:assembleDebug
```

#### iOS

```bash
xcodebuild -project 'Skydown App.xcodeproj' -scheme 'Skydown App' -destination 'generic/platform=iOS' build
```

### Aktuell wichtige Collections / Dokumente

- `users/{uid}`
- `users/{uid}/texts/{textId}`
- `users/{uid}/aiUsage/{yyyy-mm-dd}`
- `orders/{orderId}`
- `merchandise/{itemId}`
- `appConfig/paymentMethods`
- `appConfig/commerceSettings`
- `appConfig/shopifyMerch`
- `adminConfig/automationN8n`
- `adminConfig/shopifyMerchPrivate`

---

## 3. Rollen, Adminbetrieb und KI-Kostenkontrolle

### Feste Rollenlogik

Die App kennt jetzt technisch `4` gespeicherte Rollen plus den oeffentlichen Gast-Zustand:

#### 1. Owner

Das feste Hauptkonto der App.

Fuer dieses Projekt gilt:

- `nash.lioncorna@gmail.com` ist immer der `Owner`

Dieses Konto bleibt fest Owner, auch wenn in Firestore einmal etwas anderes stehen sollte. Der Code behandelt diese Mail immer als Owner-Konto.

Rechte:

- voller Zugriff auf die App
- voller Zugriff auf sensible Settings
- volle Nutzerverwaltung
- volle KI-Steuerung
- alleinige Kontrolle ueber Shopify, Zahlarten, Commerce und n8n

#### 2. Admin

Admins sind `teaminterne Leute`.

Rechte:

- operative Inhalte und Backoffice-Funktionen
- interne Betriebsunterstuetzung
- keine Kontrolle ueber Shopify, Zahlarten, Commerce, n8n oder Nutzerrollen
- keine Owner-Sonderstellung

#### 3. Subadmin

Subadmins sind `externe Power-User`, nicht das interne Kernteam.

Gedacht fuer:

- spaetere externe Partner
- vertraute Premium-Nutzer
- people mit mehr persoenlicher KI-Nutzung als normale User

Wichtig:

- `Subadmin` ist **kein** interner Admin-Workspace-Zugang
- `Subadmin` ist **kein** Staff-/Backoffice-Konto
- `Subadmin` sitzt zwischen `User` und `Admin`

#### 4. User

Normales eingeloggtes Nutzerkonto fuer die oeffentliche App.

#### Zusaetzlich: Gast

Nicht eingeloggt.

Gast ist aktuell kein gespeichertes Firestore-Rollenobjekt, sondern ein oeffentlicher Nutzungszustand ohne persoenliche Konto-History.

### Standard-Limits fuer KI

Die App hat aktuell eine serverseitige Tageslimit-Logik statt einer exakten Euro-Abrechnung.

Das bedeutet:

- es gibt pro User und pro Rolle Tagesbudgets
- die Functions zaehlen Nutzung mit
- die Clients fragen vor KI-Aktionen serverseitig an

#### Defaults

| Rolle | Bot / Tag | Visuals / Tag | Agent / Tag | History |
|---|---:|---:|---:|---:|
| Owner | 400 | 80 | 250 | 30 Tage |
| Admin | 240 | 40 | 140 | 30 Tage |
| Subadmin | 120 | 20 | 70 | 7 Tage |
| User | 30 | 4 | 18 | 3 Tage |

Diese Werte koennen pro User im User-Dokument ueberschrieben werden.

### Wie die KI-Kostenkontrolle funktioniert

#### Serverseitig

Vor Bot-, Visual- oder Agent-Nutzung laeuft ein serverseitiger Check:

- `authorizeAiUsage`

Dabei werden geprueft:

- Rolle
- ob KI fuer das Konto aktiviert ist
- wie viele Requests fuer diesen Tag schon verbraucht wurden
- welches Limit fuer diese KI-Art gilt

Die Tageszaehler liegen in:

- `users/{uid}/aiUsage/{yyyy-mm-dd}`

#### Clientseitig

Die App startet KI-Aktionen erst dann, wenn der serverseitige Check sie freigibt.

Das verhindert:

- stille Uebernutzung
- reine UI-Scheinlimits
- unterschiedliche lokale Stände zwischen Geraeten

### History

Die KI-History ist jetzt personenbezogen.

Trennung:

- pro User
- pro KI-Bereich
- mit konfigurierbarer Retention

### Owner-Workspace

Der Owner-Bereich in Settings ist jetzt als kurzer Workspace aufgebaut, nicht als endlose Scroll-Seite.

Die wichtigsten Bereiche:

- `Uebersicht`
- `Zahlungen`
- `User`
- `Shopify`
- `Versand`
- `Visuals`
- `Automation`

### Shopify fuer den Owner

Der Owner pflegt fuer Shopify:

- `Store Domain`
- `Storefront Access Token`
- optional `Collection Handle`

### n8n fuer den Owner

Der Owner pflegt die zentrale n8n-Konfiguration fuer die App.

### Wichtige Hinweise fuer das Team

- `Owner` bleibt fest an `nash.lioncorna@gmail.com` gebunden
- `Admin` ist fuer interne Leute ohne Root-/Systemkontrolle
- `Subadmin` ist fuer spaetere externe Power-User
- `User` ist das normale oeffentliche Konto
- `Gast` ist die nicht eingeloggte Nutzung ohne gespeichertes Konto

### Firebase / Sicherheit

Die wichtigsten Regeln sind:

- nur der Owner darf sensible Systembereiche wie Shopify, Zahlarten, Commerce, n8n und Nutzerrollen steuern
- interne Admins bleiben auf operative Inhalte und Backoffice-Funktionen begrenzt
- Subadmins sind bewusst keine internen Staff-Konten
- normale User duerfen nur ihre eigenen Nutzer- und Textdaten nutzen
- KI-Nutzung wird serverseitig autorisiert und gezaehlt

---

## Operative Notizen

- Android Studio bitte am Repo-Root oeffnen, nicht nur in `androidApp/`
- iOS Bundle ID: `com.skydown.ios`
- Android Package: `com.skydown.android`
- Firebase iOS Config: `Skydown App/GoogleService-Info.plist`
- Firebase Android Config: `androidApp/google-services.json`

---

## Kurzfazit

Sky²² ist jetzt technisch auf drei Ebenen lesbar:

1. als oeffentliche Medien-, Shop- und KI-App fuer Nutzer
2. als Firebase- / Shopify- / n8n-gestuetzte Mobile-Plattform fuer das Team
3. als rollebasierte App mit serverseitiger KI-Kontrolle und sauberem Adminbetrieb
