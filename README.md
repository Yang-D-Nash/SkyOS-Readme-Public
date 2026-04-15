# 22xSky

| 22 | 22xSky | Skydown |
| --- | --- | --- |
| ![22 Logo](<Skydown App/Assets.xcassets/Sky22BrandLogo.imageset/22-logo.png>) | ![22xSky](<Skydown App/Assets.xcassets/SkydownX22BrandLogo.imageset/skydown-x22-logo.png>) | ![Skydown](<Skydown App/Assets.xcassets/SkydownBrandLogo.imageset/skydown-logo.png>) |

22xSky ist die offizielle Creator-Plattform von `Skydown` und `22`.
Die App verbindet Brand, Content, Commerce und KI in einem einzigen Produkt fuer Artists, Teams und Community.

Stand: `Release Candidate` fuer produktive Tests und Rollout-Vorbereitung.

---

## 1) Unternehmen und Marken

### Skydown Entertainment

- Creative/Artist-Brand mit Fokus auf Musik, Video, Storytelling und Release-Execution.
- Operative Heimat fuer Content-Produktion, Drops und Community-Kommunikation.

### 22

- Brand-Partner im 22xSky-Universum.
- Zusammenarbeit mit Skydown in Content, Releases und Kampagnen.

### NICMA (Studio-Bereich)

- Studio-/Producer-Bereich innerhalb des Universums (kein separates Unternehmen).
- Fokus auf Beat- und Studio-Services inkl. Artist-Seiten, Kontakt und Angebotsdarstellung.

### Warum diese App

- Ein zentraler Ort fuer Artist-Auftritt, Releases, Clips, Shop und KI-Workflows.
- Rollenbasiertes Arbeiten mit klarer Trennung zwischen Owner, Team und Usern.
- Nutzbar als Plattform fuer Content-Teams und als Tool fuer einzelne Creator.

### Brand-DNA: ZweiZwei und Skydown

- Die Meisterzahl `22` (ZweiZwei) steht als Master Builder fuer Vision plus Umsetzung in reale Strukturen.
- Sie verbindet Spirit, Disziplin und praktische Wirkung.

Wer wir sind:

```
Dort, wo der Himmel faellt, beginnt unser Denken.
Was zerbricht, offenbart Tiefe - nicht Verlust.
Wir hoeren auf das, was nicht laut ist: Wandel, Stille, Sinn.
Unser Handeln wurzelt im Inneren, wo Klarheit entsteht.
Nicht im Machen liegt unsere Kraft, sondern im Verstehen.
Denn wir glauben: Der Himmel faellt nicht auf uns -
er oeffnet sich in uns.
```

- Symbolcode: `1337-514-731`
- Leet-Code: `7H3_F4LL_0F_H34/3N`
- Deutung: `THE FALL OF HEAVEN` als Motiv fuer Wandel, Oeffnung und Erkenntnis.

---

## 2) Produktueberblick

Die App ist ein `Brand + Creator Operating System` mit diesen Bereichen:

- `Home`: Einstieg, Highlights, direkte Navigation in alle Module.
- `Music`: Releases, Artists, Beat-Hub, Nicma/Studiobereich.
- `Video`: Video-Hub, Collabo-Inhalte, kuratierte Visual-Flows.
- `Merch`: Shopify-basierter Katalog mit In-App Checkout-Flow.
- `AI`: Bot (Text/Visual) plus Agent fuer umsetzungsorientierte Aufgaben.
- `Profile`: eigenes Profil, Galerie, Uploads und Personalisierung.

---

## 3) Rollen und Rechte

Fester Owner im System:

- `nash.lioncorna@gmail.com`

### Rollenmatrix (fachlich)

| Rolle | Zweck | Darf | Darf nicht |
| --- | --- | --- | --- |
| `owner` | Plattformleitung | Rollen vergeben, KI-Limits verwalten, Shopify/Payments, Runtime-Lockdown, zentrale KI/Automation-Settings | - |
| `admin` | internes Team | zugewiesene operative Bereiche (Music/Video/Profile) | keine Owner-Root-Aktionen |
| `subadmin` | Creator-/Power-User | hoehere Kontingente, eigene Nutzung | keine System-Root-Rechte |
| `user` | Standardnutzer | Profil, Galerie, Bot/Agent innerhalb Limits, eigener Workflow-Service | keine Team-/Root-Funktionen |
| `gast` | ohne Login | Lesen/Nutzen oeffentlicher Teile | keine persistente Account-Funktion |

### Rollenvergabe (technisch)

- Rollenwechsel passiert serverseitig ueber `setUserRole` (Cloud Function), nur fuer Owner.
- User-Dokumente werden beim Auth-Create automatisiert gebootstrapped.
- Owner kann pro Konto zusaetzlich Rechte und KI-Kontingente setzen.

---

## 4) KI und Agent im Produkt

### KI-Komponenten

- `Bot`: schnelle Text-/Visual-Outputs fuer Captions, Hooks, Copy und Key-Visual-Ideen.
- `Agent`: umsetzungsorientierte Antworten inkl. strukturierter Next Steps.

### Provider und Betrieb

- Serverseitiger Agent-Provider ist konfigurierbar (`Gemini` / `Manus` via Runtime Settings).
- Optional kann jeder User `Manus BYOS` aktivieren:
  - iOS: lokaler Key im Keychain.
  - Android: lokaler, verschluesselter Key (Android Keystore).
- Ohne BYOS nutzt der Agent die Backend-Konfiguration.

### Kosten- und Abuse-Schutz

- Tägliche Limits pro Konto (Text, Visual, Agent) sind serverseitig enforced.
- Hard Caps und Global Caps liegen in Runtime-Settings.
- Budget-Lockdown kann Writes/Uploads/Registrierungen zentral schalten.

---

## 5) Workflow Automation (n8n) pro User

22xSky nutzt `BYOS strict` fuer Workflows:

- Jeder Account arbeitet nur mit seiner eigenen Konfiguration:
  - `adminConfig/automationN8n_<uid>`
- Optionales persoenliches Agent-Profil:
  - `adminConfig/agentProfile_<uid>`

### Was beim Trigger passiert

Wenn ein User einen Workflow-Test oder Agent-Flow triggert:

1. Die App ruft eine auth-geschuetzte Cloud Function auf.
2. Backend laedt nur die persoenliche Workflow-Konfiguration dieses Users.
3. Bei gueltigem Setup wird ein `POST` an die n8n-Webhook-URL gesendet.
4. Payload enthaelt u. a. `trigger`, `source`, `timestamp`, `data`, optional `user`.
5. Erfolg/Fehler wird direkt als Meldung an die App zurueckgegeben.

### Erforderliche Felder im User-Setup

- `n8n aktiv`
- `Base URL`
- `Webhook Path`
- optional: `Auth Header Name/Value`
- optional: `Knowledge-Kontext`

---

## 6) Anwender-Anleitung pro Rolle

### 6.1 Owner (Plattformbetrieb)

Ziel: Systemfuehrung, Teamsteuerung, Business- und Risiko-Kontrolle.

Standardablauf:

1. `Settings > Owner` oeffnen.
2. Unter `Users` Rollen und KI-Limits pro Konto setzen.
3. Unter `Shopify / Payments / Commerce` Store und Zahlung konfigurieren.
4. Unter `AI Prompts` globale Bot-/Visual-/Agent-Anweisungen pflegen.
5. Unter `AI Runtime` Provider, Manus-Flags und Caps steuern.
6. Bei Bedarf `Runtime Lockdown` aktivieren/deaktivieren.

### 6.2 Admin (operatives Team)

Ziel: Content- und Moderationsarbeit in freigegebenen Bereichen.

Typische Aufgaben:

- Music- und Video-Inhalte pflegen.
- Profile moderieren (falls freigeschaltet).
- Artist-/Hub-Inhalte in den zugewiesenen Bereichen bearbeiten.

Nicht vorgesehen:

- Rollenvergabe.
- Owner-Commerce-Root.
- Runtime-Lockdown.

### 6.3 Subadmin / Creator

Ziel: intensivere produktive Nutzung mit hoeheren Kontingenten.

Typische Nutzung:

- Content mit Bot/Agent erstellen.
- eigenen Workflow-Service verbinden.
- persoenliches Agent-Profil mit Skills/Guardrails pflegen.

### 6.4 User (Standardkonto)

Ziel: taegliche App-Nutzung fuer Content, Profil und KI.

Schnellstart:

1. Konto registrieren / einloggen.
2. Profilbild, Bio und Galerie pflegen.
3. Bot fuer Text/Visual nutzen.
4. Agent fuer konkrete Aufgaben nutzen.
5. Optional eigenen n8n-Webhook in Settings hinterlegen.
6. Optional eigenen Manus-Key lokal aktivieren.

### 6.5 Gast

- App kann ohne Login erkundet werden.
- Keine persistente KI-/Profil-/Workflow-Funktion.

---

## 7) UX, Sprache, Offline und Feedback

### UX-Feedback

- In der App sind uebergreifend Toast-/Status-Meldungen fuer Aktionen integriert.
- Ziel ist klare Rueckmeldung bei Erfolg, Fehler und Pending-Zustaenden.

### Sprache

- Systemsprachbasiert mit 10 hinterlegten Sprachen:
  - `DE, EN, ES, FR, IT, PT, NL, PL, TR, JA`

### Offline-Verhalten

- Offline-Banner und Cache-Hinweise sind integriert.
- Wichtige Bereiche zeigen gecachte Inhalte weiter an, wenn moeglich.
- Agent-Requests koennen bei Offline-Szenarien zwischengespeichert und spaeter gesendet werden.

### Notifications

- Notification-Berechtigungen sind in der App integriert (plattformabhaengig).
- Nutzer koennen Benachrichtigungen ueber Settings verwalten.

---

## 8) Setup fuer Entwickler

### 8.1 Voraussetzungen

- Xcode (iOS Build)
- Android Studio / JDK (Android Build)
- Node.js + npm (Functions)
- Firebase CLI

### 8.2 Android Release Signing

Fuer echte Android-Release-Builds:

1. `keystore.properties.example` nach `keystore.properties` kopieren.
2. Keystore-Pfad und Passwoerter eintragen.
3. Alternativ per ENV:
   - `SKYDOWN_UPLOAD_STORE_FILE`
   - `SKYDOWN_UPLOAD_STORE_PASSWORD`
   - `SKYDOWN_UPLOAD_KEY_ALIAS`
   - `SKYDOWN_UPLOAD_KEY_PASSWORD`

Optionaler lokaler Fallback (nur fuer Smoke Tests):

```bash
./gradlew :androidApp:assembleRelease -PallowDebugReleaseSigning=true
```

### 8.3 Build-Befehle

iOS:

```bash
xcodebuild -project 'Skydown App.xcodeproj' -scheme 'Skydown App' -destination 'generic/platform=iOS' build
```

Android:

```bash
./gradlew :androidApp:compileDebugKotlin
./gradlew :androidApp:assembleDebug
```

Functions + Rules:

```bash
node --check functions/index.js
cd functions
npm run test:rules
```

Firebase Deploy:

```bash
firebase deploy --only firestore:rules
firebase deploy --only storage
firebase deploy --only functions
```

---

## 9) Security und Kostenkontrolle

Sicherheitsprinzip:

- `deny by default` in Firestore und Storage.
- Rollen- und Claim-basierte Zugriffskontrolle.
- Upload-Slot-Prinzip statt freiem Direktupload.
- App Check Rollout mit Monitor -> Enforce.

Kostenkontrolle:

- serverseitige KI-Kontingente je Konto.
- globale und harte Tages-Caps.
- Runtime-Lockdown und optionaler Budget-Lockdown.

Wichtige Runtime-Config:

- `system/runtimeConfig`
- relevante Felder: `lockdown`, `uploadsEnabled`, `registrationsEnabled`, `userWritesEnabled`, `budgetLockdownEnabled`

---

## 10) Release-Checkliste (kurz)

Vor Verteilung an Tester/Store:

1. Android Debug + Release Build gruen.
2. iOS Archive/Build gruen.
3. Functions Syntax + Rule Tests gruen.
4. Login/Registrierung inkl. User-Bootstrap pruefen.
5. Rollenwechsel Owner -> Zielkonto pruefen.
6. Bot/Visual/Agent mit Limits und Fehlermeldungen pruefen.
7. Workflow-Test pro User-Account pruefen (`automationN8n_<uid>`).
8. Shopify Sync und Checkout-Flows pruefen.
9. Offline-Verhalten und Notification-Flow pruefen.

Ergaenzende Dokumente:

- [app-check-rollout.md](app-check-rollout.md)
- [manual-test-checklist.md](manual-test-checklist.md)

---

## 11) Projektstruktur

- `Skydown App/` -> iOS App (SwiftUI)
- `androidApp/` -> Android App (Jetpack Compose)
- `shared/` -> gemeinsame Modelle/Use Cases
- `functions/` -> Firebase Cloud Functions
- `firestore.rules` -> Firestore Security Rules
- `storage.rules` -> Storage Security Rules

---

## 12) Kontakt / Betrieb

- Produkt und Betrieb: `22xSky`
- Rechtlich Verantwortlicher und Rechteinhaber:
  `Ngoc Anh Nguyen (Yang D. Nash - Skydown), Erich-Plate-Weg 44, 22419 Hamburg, Deutschland`
- Support/E-Mail (im Projekt hinterlegt): `skydownent@gmail.com`

---

## 13) Getrennte Handbuecher

Fuer schnellere Orientierung gibt es zusaetzlich:

- [README_OWNER.md](README_OWNER.md) -> Betriebs- und Owner-Handbuch
- [README_USER.md](README_USER.md) -> Endnutzer-Handbuch

Diese README bleibt der zentrale Ueberblick (`Business + Product + Operations`).

---

## 14) DSGVO + Compliance

Ein vollstaendiges operatives Compliance-Paket liegt unter:

- [docs/compliance/README.md](docs/compliance/README.md)
- [docs/compliance/DSGVO_RELEASE_CHECKLIST.md](docs/compliance/DSGVO_RELEASE_CHECKLIST.md)
- [docs/compliance/AVV_VERARBEITER_REGISTER.md](docs/compliance/AVV_VERARBEITER_REGISTER.md)
- [docs/compliance/VVT_VERARBEITUNGSTAETIGKEITEN.md](docs/compliance/VVT_VERARBEITUNGSTAETIGKEITEN.md)
- [docs/compliance/BETROFFENENRECHTE_SOP.md](docs/compliance/BETROFFENENRECHTE_SOP.md)
- [docs/compliance/DATENPANNEN_SOP.md](docs/compliance/DATENPANNEN_SOP.md)
- [docs/compliance/TOMS_CHECKLIST.md](docs/compliance/TOMS_CHECKLIST.md)
- [docs/compliance/RELEASE_READINESS_2026-04-15.md](docs/compliance/RELEASE_READINESS_2026-04-15.md)
- [docs/compliance/COMPLIANCE_REVIEW_2026-04-15.md](docs/compliance/COMPLIANCE_REVIEW_2026-04-15.md)
