# SkyOS Transparenz- und Reengineering-Dossier

Stand: 2026-04-29

Dieses Dokument beschreibt SkyOS so, dass eine technisch kompetente Person die App, das Backend,
die Datenfluesse und die Betriebslogik nachvollziehen und in einer eigenen Umgebung nachbauen kann.
Es ist bewusst transparent, aber nicht geheimnislos im falschen Sinn: echte Secrets, private Keys,
Produktionsdaten, Store-Zugaenge und personenbezogene Betreiberzugriffe gehoeren niemals in ein
oeffentliches Repository.

## 1. Transparenzprinzip

SkyOS soll erklaerbar bleiben:

- Produktidee, Architektur, Datenmodell, Build-Wege und Deploy-Wege sind dokumentiert.
- Live-Funktionen werden ehrlich von kommenden Funktionen getrennt.
- Sicherheitsgrenzen liegen serverseitig in Firebase Functions, Firestore Rules, Storage Rules,
  App Check, Rollen und Secrets.
- Oeffentliche oder freie Tools sind bevorzugt nachvollziehbar, ersetzen aber keine Plattformkonten.
- Wer SkyOS nachbauen will, braucht eigene Projekte, eigene Accounts, eigene Keys und eigene rechtliche
  Verantwortung.

Kurz: **Der Bauplan darf transparent sein; die Schluessel bleiben privat.**

## 2. Was im Repo nachvollziehbar ist

| Bereich | Quelle |
| --- | --- |
| Produkt- und Release-Uebersicht | [../README.md](../README.md) |
| Systemarchitektur | [architecture.md](architecture.md) |
| Backend, Collections, Functions, Rules | [backend.md](backend.md), [../functions/index.js](../functions/index.js), [../firestore.rules](../firestore.rules), [../storage.rules](../storage.rules) |
| iOS App | [ios.md](ios.md), [../Skydown App/](../Skydown%20App/) |
| Android App | [android.md](android.md), [../androidApp/](../androidApp/) |
| Shared Domain Layer | [../shared/](../shared/) |
| AI / Agent | [ai-system.md](ai-system.md), [automation/agent-results-contract.md](automation/agent-results-contract.md) |
| Activepieces HTTP API | [workflow-http-api-activepieces.md](workflow-http-api-activepieces.md) |
| Commerce / Payments | [commerce.md](commerce.md) |
| Deployment / Rollback | [deployment.md](deployment.md), [release/app-release-workflow.md](release/app-release-workflow.md) |
| Store-Auftritt | [store/README.md](store/README.md), [store/app-store.md](store/app-store.md), [store/google-play.md](store/google-play.md) |
| Legal / Trust | [legal/](legal/), [compliance/README.md](compliance/README.md), [../site/](../site/) |
| Brand / UI / Produktqualitaet | [branding.md](branding.md), [skyos-product-design-audit.md](skyos-product-design-audit.md) |

## 3. Was nicht oeffentlich in Git gehoert

Diese Dinge sind absichtlich nicht rekonstruierbar:

- echte Firebase Service-Account-Dateien
- echte Firebase Secrets und Runtime-Secret-Werte
- Apple Developer Team-Zugriff, Zertifikate, Provisioning Profiles und App Store Connect Sessions
- Google Play Console Zugriff und Upload Keys
- echte Android Keystore-Passwoerter
- Stripe Secret Keys und Webhook Secrets
- Shopify Admin Tokens
- SMTP Zugangsdaten
- AI Provider Keys
- Activepieces Workflow Secret und Callback Secret
- Produktionsdaten, User-Exports, Logs mit personenbezogenen Inhalten
- private Betreiber-, Vertrags- oder Bankunterlagen

Im Repo duerfen nur Namen, Platzhalter, Schemas, Befehle und Beispielwerte stehen.

## 4. Account- und Infrastruktur-Matrix

| Bereich | Warum noetig | Oeffentlich / frei nachvollziehbar | Eigenes Konto erforderlich |
| --- | --- | --- | --- |
| Git + Source | Code, Docs, Versionsgeschichte | Ja | GitHub/Git-Host fuer Zusammenarbeit |
| Xcode | iOS Build lokal | Ja, lokal installierbar | Apple Developer Account fuer Store, Signing, TestFlight |
| Android Studio / Gradle | Android Build lokal | Ja | Google Play Console fuer Store-Release |
| Firebase / Google Cloud | Auth, Firestore, Storage, Functions, App Check, FCM | Doku + Emulatoren ja | eigenes Firebase/GCP Projekt, ggf. Billing |
| Apple Push / FCM | Reminder Push auf iOS/Android | Architektur ja | eigene Bundle IDs, APNs/Firebase-Konfiguration |
| Activepieces | externe Workflow-Erstellung von Reminder/Task/Note | API-Vertrag ja | eigener Activepieces Flow oder Cloud/Self-Host |
| Stripe / Klarna | Checkout, Webhooks, Payment Status | Contract und Code ja | eigener Stripe Account und aktivierte Payment Methods |
| Shopify | Merch-Katalog und Order-Kontext | Storefront/Admin-Logik ja | eigener Shop und Tokens |
| SMTP | Support-/Order-Mail | Mail-Flow ja | eigener Mailprovider |
| AI Provider | Bot, Visuals, Agent Runtime | Prompt-/Function-Logik ja | eigene Provider-Keys und Kostenkontrolle |
| Store Legal URLs | Privacy, Terms, Support | Seiten im Repo ja | eigene Domain/Hosting/Store-Konfiguration |

## 5. Reengineering-Pfad von null

1. **Repo klonen**
   - Code auschecken.
   - Keine echten Secrets aus anderen Umgebungen uebernehmen.

2. **Toolchain installieren**
   - JDK 17 fuer Android/KMP.
   - Node 22 fuer `functions/`.
   - Xcode fuer iOS.
   - Firebase CLI fuer Emulatoren und Deploys.

3. **Eigenes Firebase-Projekt anlegen**
   - Auth aktivieren.
   - Firestore und Storage aktivieren.
   - Cloud Functions vorbereiten.
   - App Check Strategie fuer Debug/Prod definieren.
   - iOS App mit Bundle ID und Android App mit Package registrieren.
   - `GoogleService-Info.plist` und `google-services.json` fuer das eigene Projekt erzeugen.

4. **Lokale Secrets nur lokal setzen**
   - `.env.example` als Vorlage nutzen.
   - Firebase Secrets mit `firebase functions:secrets:set <NAME>` setzen.
   - Android Keystore lokal/CI-secret halten.

5. **Backend pruefen**
   - `npm --prefix functions ci`
   - `npm --prefix functions run build`
   - `npm --prefix functions test`
   - Firestore/Storage Rules via Emulator testen.

6. **Clients bauen**
   - Android Debug: `./gradlew :androidApp:assembleDebug`
   - Android Release nach Runbook: [android.md](android.md)
   - iOS Simulator Build: siehe [ios.md](ios.md)
   - Store-Signing erst mit eigenen Konten und Zertifikaten.

7. **Rules und Functions deployen**
   - `firebase deploy --only firestore:rules,storage`
   - `firebase deploy --only firestore:indexes`
   - `firebase deploy --only functions`

8. **Seed / Runtime Config setzen**
   - Owner Account anlegen.
   - Rollen/Claims synchronisieren.
   - `system/runtimeConfig` pruefen.
   - `adminConfig/*` nur mit bewusstem Scope setzen.

9. **Integrationen anschliessen**
   - Activepieces Secret und Endpoints konfigurieren.
   - Stripe/Shopify/SMTP/AI Provider erst aktivieren, wenn echte Konten bereit sind.

10. **Release-Smoke**
    - Reminder + Push realgeraetig pruefen.
    - Task/Note lokal und ueber Activepieces pruefen.
    - Store URLs, Datenschutz, Terms, Support pruefen.

## 6. Kern-Datenmodell in einem Satz

SkyOS speichert Nutzer- und Produktivitaetsdaten primar unter `users/{uid}/...`, oeffentliche oder
owner-gesteuerte Inhalte in eigenen Top-Level-Sammlungen, und privilegierte Systemkonfiguration unter
`adminConfig/*` bzw. `system/*`.

Wichtige Pfade:

| Pfad | Zweck |
| --- | --- |
| `users/{uid}` | Profil, Rolle, Plan, Limits, Account-Kontext |
| `users/{uid}/reminders/{id}` | Reminder inkl. `scheduledAt`, `timezone`, `status`, `source` |
| `users/{uid}/tasks/{id}` | Aufgaben mit Status, Prioritaet, optionalem Due-Date |
| `users/{uid}/notes/{id}` | Notizen, Quelle, optionales Ablauf-/Retention-Verhalten |
| `users/{uid}/pushTokens/{tokenId}` | FCM/APNs Routing fuer Push |
| `users/{uid}/agentRuns/{runId}` | Agent-Laufstatus und Korrelation |
| `adminConfig/aiPromptSettings` | zentrale AI-/Agent-Promptsteuerung |
| `adminConfig/automationN8n_{uid}` | nutzerbezogene Automation-Konfiguration |
| `system/runtimeConfig` | Lockdown, App Check, Feature-/Safety-Schalter |
| `orders/{orderId}` | Commerce-/Bestellkontext |
| `legalContent/*` bzw. Admin Legal Config | oeffentliche Rechtstext- und Betreiberdefaults |

Die finale Autoritaet liegt nicht in der UI, sondern in Rules und Functions.

## 7. Live vs. Coming Next

| Bereich | Status |
| --- | --- |
| Reminder + Push | Live, mit App-Write, Firestore, Scheduled Function und FCM/APNs-Pfad |
| Tasks | Live in App und via Activepieces HTTP API |
| Notes | Live in App und via Activepieces HTTP API |
| Activepieces HTTP API | Live fuer Reminder/Task/Note mit `x-skyos-workflow-secret` |
| Memory / tiefere Folgeautomation | Coming next, nicht als voll live bewerben |
| Store Payments / Subscriptions | technisch vorbereitet; echte Store-/Provider-Konfiguration entscheidet ueber Live-Status |

## 8. Sicherheitsmodell fuer Nachbauer

- Nutze eigene Firebase-Projekte und eigene App IDs.
- Setze Secrets nur in Firebase Secret Manager, CI Secrets oder lokaler Shell.
- Committe keine `.env`, keine Keystores, keine Tokens.
- Teste Firestore/Storage Rules vor jedem Deploy.
- Behandle Client-Konfigurationen als projektbezogen; sie sind nicht dasselbe wie Server-Secrets.
- Aktiviere produktive App Check Enforcement erst nach Debug-/Store-Setup.
- Setze Budget Alerts und Provider-Limits, bevor AI/Payments oeffentlich genutzt werden.

## 9. Mindestchecks fuer eine nachvollziehbare Kopie

```bash
npm --prefix functions run build
npm --prefix functions test
./gradlew :androidApp:assembleDebug
xcodebuild -project "Skydown App.xcodeproj" -scheme "Skydown App" -configuration Debug -destination "generic/platform=iOS Simulator" CODE_SIGNING_ALLOWED=NO build
npx --yes html-validate site/index.html site/privacy.html site/terms.html site/support.html
```

Je nach lokaler Maschine und installierten Tools sind iOS, Android oder Firebase Emulatoren separat
vorzubereiten. Store-Releases brauchen zusaetzlich echte Konsolen- und Signing-Zugaenge.

## 10. Transparenz ohne falsche Garantie

Dieses Dossier macht SkyOS nachvollziehbar. Es garantiert nicht:

- dass fremde Store-Accounts genehmigt werden
- dass externe Provider gleiche Konditionen oder Limits bieten
- dass ein Nachbau rechtlich automatisch freigegeben ist
- dass produktive Daten migriert werden koennen
- dass echte Push-/Payment-/Subscription-Flows ohne Plattformfreigaben laufen

Der sinnvolle Standard ist: **Bauplan offen, Betrieb verantwortet, Secrets privat.**
